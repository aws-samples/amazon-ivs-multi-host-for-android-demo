package com.amazon.ivs.stagebroadcastmanager

import android.annotation.SuppressLint
import android.content.Context
import com.amazon.ivs.stagebroadcastmanager.common.*
import com.amazon.ivs.stagebroadcastmanager.extensions.asParticipantItem
import com.amazon.ivs.stagebroadcastmanager.extensions.getSortedCameraDeviceList
import com.amazon.ivs.stagebroadcastmanager.extensions.getUsername
import com.amazon.ivs.stagebroadcastmanager.extensions.indexOrNull
import com.amazon.ivs.stagebroadcastmanager.models.BroadcastState
import com.amazon.ivs.stagebroadcastmanager.models.ConnectedParticipant
import com.amazon.ivs.stagebroadcastmanager.models.StageEvent
import com.amazonaws.ivs.broadcast.*
import com.amazonaws.ivs.broadcast.BroadcastConfiguration.AspectMode
import com.amazonaws.ivs.broadcast.BroadcastConfiguration.Mixer.Slot
import com.amazonaws.ivs.broadcast.Device.Descriptor
import com.amazonaws.ivs.broadcast.Presets.Devices
import com.amazonaws.ivs.broadcast.Stage.Strategy
import kotlinx.coroutines.flow.*
import timber.log.Timber

interface StageBroadcastManager {
    // Functionality related to the participant own stream
    /**
     * Emits false when microphone is turned on and emits true when microphone turned off
     */
    val onMicrophoneMuted: StateFlow<Boolean>

    /**
     * Emits false when camera is turned on and emits true when camera turned off
     */
    val onVideoMuted: StateFlow<Boolean>

    /**
     * Function toggles video source (camera) and updates [onVideoMuted] flow
     */
    fun toggleVideo()

    /**
     * Function toggles audio source (microphone) and updates [onMicrophoneMuted] flow
     */
    fun toggleAudio()

    /**
     * Swaps front and back camera, and automatically emits [StageEvent.PreviewUpdated] event in [stageEvents] flow if
     * video source was changed and local video updated.
     * Does nothing if local video is turned off.
     */
    fun flipLocalCameraDirection()

    /**
     * Forces update of the local user video output and emits [StageEvent.PreviewUpdated] event in [stageEvents] flow if
     * preview was successfully reloaded
     */
    fun reloadCameraPreview()

    // Functionality related to Stage
    /**
     * Flow is responsible for emitting Stage related events like:
     *
     * : [StageEvent.AddParticipantView] with information about new participant of the Stage and his video preview
     * : [StageEvent.RemoveParticipantView] with the id of participant whose video preview needs to be removed.
     *  For example, when user turns off his video or user left the Stage.
     * : [StageEvent.PreviewUpdated] with new preview for the user. For example, when local user switches the video
     *  source from back camera to front.
     * : [StageEvent.StageClosed] when Stage has been closed locally
     * : [StageEvent.StageDisconnected] when user has been disconnected from the Stage
     */
    val stageEvents: SharedFlow<StageEvent>

    /**
     * Flow emits all connected participants to the Stage
     */
    val connectedParticipants: StateFlow<List<ConnectedParticipant>>

    /**
     * Use to join a stage by providing stream token. If there is no stage, it will be created automatically
     * @param token the stream token
     */
    fun joinStage(token: String)

    // Functionality related to other participant states
    /**
     * Toggles audio of connected participant for the local user: not affecting other users.
     *
     * NOTE: due to the SDK not supporting this, it is purely visual
     */
    fun toggleMuteParticipant(participant: ConnectedParticipant)

    /**
     * Toggles video of connected participant for the local user: not affecting other users view.
     */
    fun hideVideoOfParticipant(participant: ConnectedParticipant)

    // Functionality related to broadcasting
    /**
     * Flow is responsible for emitting state of the Broadcast:
     * States are: [BroadcastState.BROADCASTING], [BroadcastState.LOADING], [BroadcastState.NOT_BROADCASTING]
     */
    val broadcastState: StateFlow<BroadcastState>

    /**
     * Use to create Broadcast Session that also automatically assigns local devices for video and audio streams.
     * @param isPreview used for creating session for preview. If true, should prevent from using actual logic and
     * assigning listeners.
     * Also changes the aspect ratio of user's video output: [BroadcastConfiguration.AspectMode.FIT] or
     * [BroadcastConfiguration.AspectMode.FILL]
     */
    fun createBroadcastSession(isPreview: Boolean = false)

    /**
     * Use to start the session broadcasting.
     * @param key stream key
     * @param endpoint ingest endpoint for the stream
     */
    fun startBroadcast(key: String, endpoint: String)

    /**
     * Use to stop session broadcasting.
     */
    fun stopBroadcast()

    /**
     * Use to stop and leave the Broadcast Session, as well as leave the Stage and release all the streams (audio and
     * video) and detach devices from the session.
     */
    fun release()
}

@SuppressLint("NewApi")
class StageBroadcastManagerImpl(
    private val context: Context,
    private val localUsername: String?,
    private val localAvatarUrl: String?
) : StageBroadcastManager {
    private val _stageEvents = MutableSharedFlow<StageEvent>(replay = 0, extraBufferCapacity = 1)
    private val _connectedParticipants = MutableStateFlow<List<ConnectedParticipant>>(emptyList())
    private val _onMicrophoneMuted = MutableStateFlow(false)
    private val _onVideoMuted = MutableStateFlow(false)

    // Device related properties
    private var cameraDevice: Device? = null
    private var micDevice: AudioDevice? = null
    private var videoStream: ImageLocalStageStream? = null
    private var microphoneStream: AudioLocalStageStream? = null
    private var isBackCamera = false
    private var isPreviewing: Boolean = false
    private val deviceDiscovery = DeviceDiscovery(context)

    // Stage related properties
    private var localStage: Stage? = null
    private val strategy = object : Strategy {
        // Adding our own streams here will trigger the onStreamsAdded/-Removed inside stageRenderer object
        override fun stageStreamsToPublishForParticipant(
            stage: Stage,
            participantInfo: ParticipantInfo
        ): List<LocalStageStream> {
            val streams = ArrayList<LocalStageStream>()
            videoStream?.run {
                streams.add(this)
            }
            microphoneStream?.run {
                streams.add(this)
            }
            Timber.d("Stage streams to publish: $streams for participant: ${participantInfo.getUsername()}")
            return streams
        }

        override fun shouldPublishFromParticipant(stage: Stage, participantInfo: ParticipantInfo): Boolean {
            Timber.d("Should publish from participant: ${participantInfo.getUsername()}")
            return true
        }

        override fun shouldSubscribeToParticipant(stage: Stage, participantInfo: ParticipantInfo): Stage.SubscribeType {
            Timber.d("Should subscribe to participant: ${participantInfo.getUsername()}")
            return Stage.SubscribeType.AUDIO_VIDEO
        }
    }
    private val stageRenderer = object : AnalyticsStageRenderer {
        override fun onError(error: BroadcastException) {
            error.run {
                Timber.e(error, "Stage error: $detail, code: $code, source: $source")
            }
        }

        override fun onConnectionStateChanged(
            stage: Stage,
            state: Stage.ConnectionState,
            exception: BroadcastException?
        ) {
            Timber.d("Stage connection state changed: $state")
            exception?.run { onError(this) }
        }

        override fun onParticipantJoined(stage: Stage, participantInfo: ParticipantInfo) {
            Timber.d("Participant: ${participantInfo.getUsername()} joined the stage")
            if (!participantInfo.isLocal) {
                _connectedParticipants.updateList {
                    this.add(
                        participantInfo.asParticipantItem(
                            isLocalVideoStopped = false,
                            isLocalAudioMuted = false,
                            isRemoteVideoStopped = true,
                            isRemoteAudioMuted = true
                        )
                    )
                }
            } else {
                _connectedParticipants.updateList {
                    var localUser = participantInfo.asParticipantItem(
                        isLocalAudioMuted = _onMicrophoneMuted.value,
                        isLocalVideoStopped = _onVideoMuted.value,
                        isRemoteAudioMuted = _onMicrophoneMuted.value,
                        isRemoteVideoStopped = _onVideoMuted.value,
                    )
                    localUsername?.let { localUser = localUser.copy(name = it) }
                    localAvatarUrl?.let { localUser = localUser.copy(iconUrl = it) }
                    this.add(localUser)
                }
            }
            updateBroadcastBindings()
        }

        override fun onParticipantLeft(stage: Stage, participantInfo: ParticipantInfo) {
            Timber.d("Participant: ${participantInfo.getUsername()} left the stage")
            if (participantInfo.isLocal) {
                _connectedParticipants.updateList {
                    val participantIndex = indexOrNull(participantInfo) ?: return@updateList
                    this.removeAt(participantIndex)
                }
                launchMain {
                    releaseResources()
                    _stageEvents.tryEmit(StageEvent.StageDisconnected)
                }
            } else {
                removeParticipant(participantInfo.participantId)
                _connectedParticipants.updateList {
                    val participantIndex = indexOrNull(participantInfo) ?: return@updateList
                    this.removeAt(participantIndex)
                }
            }
            updateBroadcastBindings()
        }

        override fun onParticipantSubscribeStateChanged(
            stage: Stage,
            participantInfo: ParticipantInfo,
            publishState: Stage.SubscribeState
        ) {
            Timber.d("Participant: ${participantInfo.getUsername()} changed publish state: $publishState")
        }

        override fun onParticipantPublishStateChanged(
            stage: Stage,
            participantInfo: ParticipantInfo,
            publishState: Stage.PublishState
        ) {
            Timber.d("Participant: ${participantInfo.getUsername()} changed publish state: $publishState")
        }

        override fun onStreamsAdded(stage: Stage, participantInfo: ParticipantInfo, streams: MutableList<StageStream>) {
            streams.forEach { stream ->
                Timber.d("Participant: ${participantInfo.getUsername()} stream added: ${stream.streamType}")
                if (stream.streamType == StageStream.Type.VIDEO && !participantInfo.isLocal) {
                    _connectedParticipants.updateList {
                        val participantIndex = indexOrNull(participantInfo) ?: return@updateList
                        this[participantIndex] = this[participantIndex].copy(
                            isRemoteVideoStopped = stream.muted
                        )
                    }

                    try {
                        val videoPreview = (stream.device as ImageDevice).getPreviewView(AspectMode.FILL)
                        addParticipantWithPreview(participantInfo.participantId, videoPreview)

                        session?.attachDevice(stream.device)
                        session?.mixer?.addSlot(createParticipantSlot(participantInfo.participantId, 1))
                        session?.mixer?.bind(stream.device, participantInfo.participantId)
                        updateBroadcastBindings()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to get video preview for participant: ${participantInfo.getUsername()}")
                    }
                }

                if (stream.streamType == StageStream.Type.AUDIO && !participantInfo.isLocal) {
                    _connectedParticipants.updateList {
                        val participantIndex = indexOrNull(participantInfo) ?: return@updateList
                        val localParticipant = this[participantIndex]
                        this[participantIndex] = localParticipant.copy(
                            isRemoteAudioMuted = stream.muted,
                            device = stream.device
                        )
                    }
                    session?.attachDevice(stream.device)
                    session?.mixer?.bind(stream.device, participantInfo.participantId)
                }
            }
        }

        override fun onStreamsRemoved(
            stage: Stage,
            participantInfo: ParticipantInfo,
            streams: MutableList<StageStream>
        ) {
            streams.forEach { stream ->
                Timber.d("Participant: ${participantInfo.getUsername()} with id: ${participantInfo.participantId}, stream removed: ${stream.streamType}")
                if (stream.streamType == StageStream.Type.VIDEO && !participantInfo.isLocal) {
                    _connectedParticipants.updateList {
                        val participantIndex = indexOrNull(participantInfo) ?: return@updateList
                        this[participantIndex] = this[participantIndex].copy(
                            isRemoteVideoStopped = true,
                            isLocalVideoStopped = true
                        )
                    }
                    session?.detachDevice(stream.device)
                    session?.mixer?.removeSlot(participantInfo.participantId)
                    session?.mixer?.unbind(stream.device)
                    updateBroadcastBindings()
                }

                if (stream.streamType == StageStream.Type.AUDIO && !participantInfo.isLocal) {
                    _connectedParticipants.updateList {
                        val participantIndex = indexOrNull(participantInfo) ?: return@updateList
                        this[participantIndex] = this[participantIndex].copy(
                            isRemoteAudioMuted = true,
                            isLocalAudioMuted = true
                        )
                    }
                    session?.detachDevice(stream.device)
                    session?.mixer?.unbind(stream.device)
                }
            }
        }

        override fun onStreamsMutedChanged(
            stage: Stage,
            participantInfo: ParticipantInfo,
            streams: MutableList<StageStream>
        ) {
            streams.forEach { stream ->
                Timber.d("Participant: ${participantInfo.getUsername()} stream mute changed: ${stream.streamType}")
                if (stream.streamType == StageStream.Type.VIDEO && !participantInfo.isLocal) {
                    _connectedParticipants.updateList {
                        val participantIndex = indexOrNull(participantInfo) ?: return@updateList
                        this[participantIndex] = this[participantIndex].copy(
                            isRemoteVideoStopped = stream.muted
                        )
                    }
                    updateBroadcastBindings()
                }

                if (stream.streamType == StageStream.Type.AUDIO && !participantInfo.isLocal) {
                    _connectedParticipants.updateList {
                        val participantIndex = indexOrNull(participantInfo) ?: return@updateList
                        val localParticipant = this[participantIndex]
                        this[participantIndex] = localParticipant.copy(
                            isRemoteAudioMuted = stream.muted
                        )
                    }
                }
            }
        }

        override fun onAnalyticsEvent(name: String, properties: String) {
            Timber.d("Analytics event: $name, $properties")
        }
    }

    // Broadcast related properties
    private var session: BroadcastSession? = null
    private val _broadcastState = MutableStateFlow(BroadcastState.NOT_BROADCASTING)
    private val broadcastListener by lazy {
        (object : BroadcastSession.Listener() {
            override fun onDeviceAdded(descriptor: Descriptor) {
                Timber.d("Device added: ${descriptor.type}")
            }

            override fun onDeviceRemoved(descriptor: Descriptor) {
                Timber.d("Device removed: ${descriptor.type}")
            }

            override fun onStateChanged(state: BroadcastSession.State) {
                Timber.d("State changed: $state")
                when (state) {
                    BroadcastSession.State.CONNECTED -> _broadcastState.update { BroadcastState.BROADCASTING }
                    BroadcastSession.State.DISCONNECTED -> _broadcastState.update { BroadcastState.NOT_BROADCASTING }
                    BroadcastSession.State.CONNECTING -> _broadcastState.update { BroadcastState.LOADING }
                    BroadcastSession.State.ERROR,
                    BroadcastSession.State.INVALID -> _broadcastState.update { BroadcastState.NOT_BROADCASTING }
                }
            }

            override fun onError(error: BroadcastException) {
                error.run {
                    Timber.e(error, "Broadcast error: $detail, code: $code, source: $source")
                }
            }
        })
    }

    override val stageEvents = _stageEvents.asSharedFlow()
    override val connectedParticipants = _connectedParticipants.asStateFlow()
    override val onMicrophoneMuted = _onMicrophoneMuted.asStateFlow()
    override val onVideoMuted = _onVideoMuted.asStateFlow()
    override val broadcastState = _broadcastState.asStateFlow()

    override fun startBroadcast(key: String, endpoint: String) {
        try {
            session?.start(endpoint, key)
            Timber.d("Broadcast started")
        } catch (e: BroadcastException) {
            Timber.e(e, "Failed to start broadcast")
        }
    }

    override fun stopBroadcast() {
        session?.stop()
        Timber.d("Broadcast stopped")
    }

    override fun joinStage(token: String) {
        Timber.d("Joining stage with token: $token")
        if (localStage == null) {
            createStage(token)
        }
        localStage?.join()
        isPreviewing = false
    }

    override fun hideVideoOfParticipant(participant: ConnectedParticipant) {
        // In our case, this will just turn off showing the view in the fragment
        _connectedParticipants.updateList {
            val participantIndex = indexOrNull(participant) ?: return@updateList
            this[participantIndex] = this[participantIndex].copy(
                isLocalVideoStopped = !this[participantIndex].isLocalVideoStopped
            )
        }
    }

    override fun toggleMuteParticipant(participant: ConnectedParticipant) {
        // TODO: SDK doesn't properly support muting audio
        _connectedParticipants.updateList {
            val participantIndex = indexOrNull(participant) ?: return@updateList
            this[participantIndex] = this[participantIndex].copy(
                isLocalAudioMuted = !this[participantIndex].isLocalAudioMuted
            ).apply {
                device?.run {
                    Timber.d("Mute device: $isLocalAudioMuted")
                    // TODO: has no effect
                    if (isLocalAudioMuted) {
                        session?.detachDevice(this)
                    } else {
                        session?.attachDevice(this)
                    }
                }
            }
        }
    }

    override fun createBroadcastSession(isPreview: Boolean) {
        session?.release()

        Timber.d("Creating broadcast session")
        isPreviewing = isPreview
        val broadcastConfig = BroadcastConfiguration().apply {
            val cameraSlot = createParticipantSlot(SELF_PARTICIPANT_ID, 1).apply {
                aspect = AspectMode.FIT
                preferredVideoInput = Descriptor.DeviceType.CAMERA
                preferredAudioInput = Descriptor.DeviceType.MICROPHONE
            }
            this.mixer.slots = arrayOf(cameraSlot)
            this.logLevel = BroadcastConfiguration.LogLevel.DEBUG
        }
        var listener: BroadcastSession.Listener? = null
        if (!isPreviewing) listener = broadcastListener
        val startDevices = if (isBackCamera) Devices.BACK_CAMERA(context) else Devices.FRONT_CAMERA(context)
        BroadcastSession(context, listener, broadcastConfig, startDevices).apply {
            if (isReady) {
                session = this
                awaitDeviceChanges {
                    try {
                        val preview = if (_onVideoMuted.value) null else this.previewView
                        addParticipantWithPreview(SELF_PARTICIPANT_ID, preview)
                        initVideoAndAudioStreams()
                    } catch (ex: BroadcastException) {
                        Timber.d("Unable to display image preview")
                    }
                }
            } else {
                Timber.d("Broadcast session not ready")
            }
        }
    }

    override fun release() {
        releaseResources()
        val event = _stageEvents.tryEmit(StageEvent.StageClosed)
        Timber.d("Emitted stage close event: $event")
    }

    override fun flipLocalCameraDirection() {
        val devicePosition = if (cameraDevice?.descriptor?.position == Descriptor.Position.BACK)
            FRONT_CAMERA_POSITION else BACK_CAMERA_POSITION
        val newCamera = deviceDiscovery.getSortedCameraDeviceList()[devicePosition]
        val canFlip = !_onVideoMuted.value && cameraDevice?.descriptor?.isValid == true
        if (!canFlip) return

        cameraDevice?.takeIf { it.isValid }?.let { oldCamera ->
            ImageLocalStageStream(newCamera, null).apply {
                videoStream = this
                videoStream?.muted = _onVideoMuted.value
            }
            session?.exchangeDevices(oldCamera, newCamera.descriptor) { device ->
                Timber.d("Cameras exchanged from: ${oldCamera.descriptor?.friendlyName} to: ${device.descriptor?.friendlyName}")
                isBackCamera = devicePosition == BACK_CAMERA_POSITION
                cameraDevice = device
                reloadCameraPreview()
                localStage?.refreshStrategy()
            }
        }
    }

    override fun reloadCameraPreview() {
        (cameraDevice as? ImageDevice)
            ?.takeIf { it.isValid }
            ?.getPreviewView(if (isPreviewing) AspectMode.FIT else AspectMode.FILL)
            ?.let { newPreview ->
                Timber.d("Updated camera preview")
                _stageEvents.tryEmit(StageEvent.PreviewUpdated(newPreview, SELF_PARTICIPANT_ID))
            }
    }

    override fun toggleVideo() {
        if (_onVideoMuted.value) {
            videoStream?.muted = false
            cameraDevice?.run {
                session?.attachDevice(this)
                reloadCameraPreview()
            }
            _onVideoMuted.update { false }
            Timber.d("Local video is ON")
        } else {
            videoStream?.muted = true
            cameraDevice?.run {
                session?.detachDevice(this)
            }
            _onVideoMuted.update { true }
            Timber.d("Local video is OFF")
        }
        _connectedParticipants.updateList {
            val participantIndex = indexOfFirst { it.isLocal }
            if (participantIndex == -1) return@updateList
            this[participantIndex] = this[participantIndex].copy(
                isRemoteVideoStopped = _onVideoMuted.value,
                isLocalVideoStopped = _onVideoMuted.value
            )
        }
    }

    override fun toggleAudio() {
        if (_onMicrophoneMuted.value) {
            microphoneStream?.muted = false
            micDevice?.setGain(2f)
            _onMicrophoneMuted.update { false }
            Timber.d("Local microphone is ON")
        } else {
            microphoneStream?.muted = true
            micDevice?.setGain(0f)
            _onMicrophoneMuted.update { true }
            Timber.d("Local microphone is OFF")
        }
        _connectedParticipants.updateList {
            val participantIndex = indexOfFirst { it.isLocal }
            if (participantIndex == -1) return@updateList
            this[participantIndex] = this[participantIndex].copy(
                isRemoteAudioMuted = _onMicrophoneMuted.value,
                isLocalAudioMuted = _onMicrophoneMuted.value
            )
        }
    }

    private fun releaseResources() {
        Timber.d("Releasing broadcast manager and local stage")
        session?.stop()
        session?.listAttachedDevices()?.forEach { device ->
            Timber.d("Detaching device: ${device.descriptor.friendlyName}")
            session?.detachDevice(device)
            session?.mixer?.unbind(device)
        }
        session?.mixer?.removeSlot(SELF_PARTICIPANT_ID)

        Timber.d("Releasing broadcaster")
        session?.release()
        localStage?.leave()
        session = null
        localStage = null

        videoStream?.muted = true
        microphoneStream?.muted = true
        videoStream = null
        microphoneStream = null
        cameraDevice = null
        micDevice = null
        Timber.d("Broadcast manager released")
    }

    private fun createStage(token: String) {
        Stage(context, token, strategy).apply {
            localStage = this
            addRenderer(stageRenderer)
        }
        Timber.d("Stage has been created")
    }

    private fun initVideoAndAudioStreams() {
        session?.listAttachedDevices()?.forEach { device ->
            Timber.d("Connected device: $device")
            if (device.descriptor.type == Descriptor.DeviceType.CAMERA) {
                cameraDevice = device
                cameraDevice?.run {
                    ImageLocalStageStream(this, null).apply {
                        videoStream = this
                        videoStream?.muted = _onVideoMuted.value
                    }
                }
                session?.mixer?.bind(device, SELF_PARTICIPANT_ID)
                Timber.d("Camera device added with video stream")
                reloadCameraPreview()
            }

            if (device.descriptor.type == Descriptor.DeviceType.MICROPHONE) {
                micDevice = device as AudioDevice
                micDevice?.run {
                    AudioLocalStageStream(this).apply {
                        microphoneStream = this
                        microphoneStream?.muted = _onMicrophoneMuted.value
                    }
                }
                session?.mixer?.bind(device, SELF_PARTICIPANT_ID)
                Timber.d("Audio device added with microphone stream")
            }
        }
    }

    private fun addParticipantWithPreview(participantId: String, preview: ImagePreviewView?) {
        var name = localUsername ?: ""
        var avatar = localAvatarUrl ?: ""
        var isVideoOff = preview == null
        _connectedParticipants.value.find { it.id == participantId }?.let { connectedParticipant ->
            name = connectedParticipant.name
            avatar = connectedParticipant.iconUrl
            isVideoOff = connectedParticipant.isVideoOff
        }

        Timber.d("Sending broadcast event to add participant: $participantId")
        _stageEvents.tryEmit(
            StageEvent.AddParticipantView(
                viewToAdd = preview,
                participantId = participantId,
                name = name,
                avatar = avatar,
                isVideoOff = isVideoOff
            )
        )
    }

    private fun removeParticipant(participantId: String) {
        Timber.d("Unsubscribe from participant: $participantId")
        _stageEvents.tryEmit(StageEvent.RemoveParticipantView(participantId))
        session?.mixer?.removeSlot(participantId)
    }

    private fun createParticipantSlot(name: String, zIndex: Int): Slot =
        Slot.with { config ->
            config.name = name
            config.size = STAGE_SIZE
            config.aspect = AspectMode.FILL
            config.position = BroadcastConfiguration.Vec2(0f, 0f)
            config.fillColor = BroadcastConfiguration.Vec4(0f, 0f, 0f, 0f)
            config.setzIndex(zIndex)
            return@with config
        }

    private fun updateBroadcastBindings() {
        session?.updateSlotLayout(_connectedParticipants.value.map { if (it.isLocal) SELF_PARTICIPANT_ID else it.id })
    }
}
