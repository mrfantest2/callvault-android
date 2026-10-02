package win.fantest.callvault

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.text.InputType
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import win.fantest.callvault.core.backup.EncryptedBackupManager
import win.fantest.callvault.core.calls.CallSessionTracker
import win.fantest.callvault.core.calls.CallStateMonitor
import win.fantest.callvault.core.metadata.SimInventory
import win.fantest.callvault.core.recorder.MicrophoneRecorderEngine
import win.fantest.callvault.core.recorder.RecorderState
import win.fantest.callvault.core.retention.RetentionPolicyRepository
import win.fantest.callvault.core.rules.RecordingRulesRepository
import win.fantest.callvault.core.storage.RecordingEntry
import win.fantest.callvault.core.storage.RecordingLibrary
import win.fantest.callvault.service.AutoRecordingService
import java.io.File

class MainActivity : Activity() {
    private val recorder by lazy { MicrophoneRecorderEngine(this) }
    private val backupManager by lazy { EncryptedBackupManager(this) }
    private val library by lazy { RecordingLibrary(this) }
    private val simInventory by lazy { SimInventory(this) }
    private val rulesRepository by lazy { RecordingRulesRepository(this) }
    private val retentionRepository by lazy { RetentionPolicyRepository(this) }
    private val sessionTracker = CallSessionTracker()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var statusView: TextView
    private lateinit var callStateView: TextView
    private lateinit var simView: TextView
    private lateinit var libraryView: TextView
    private lateinit var listContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var seekBar: SeekBar
    private lateinit var playbackView: TextView
    private lateinit var modeButton: Button

    private var pendingStart = false
    private var pendingSimRefresh = false
    private var currentQuery = ""
    private var showTrash = false
    private var player: MediaPlayer? = null
    private var playingEntry: RecordingEntry? = null

    private val progressUpdater = object : Runnable {
        override fun run() {
            val activePlayer = player
            if (activePlayer != null) {
                seekBar.max = activePlayer.duration.coerceAtLeast(1)
                seekBar.progress = activePlayer.currentPosition
                playbackView.text =
                    "Playback: " + formatMs(activePlayer.currentPosition.toLong()) +
                        " / " + formatMs(activePlayer.duration.toLong())
                handler.postDelayed(this, 500)
            }
        }
    }

    private val callMonitor by lazy {
        CallStateMonitor(this) { state ->
            runOnUiThread {
                val completed = sessionTracker.onStateChanged(state)
                callStateView.text = "Call state: " + state.name
                if (completed != null) {
                    statusView.text = "Call session completed: " + completed.id.take(8)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val purged = library.purgeExpiredTrash(retentionRepository.trashRetentionDays())

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 48)
        }
        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(TextView(this).apply {
            text = "CallVault"
            textSize = 30f
        })

        statusView = TextView(this).apply {
            text =
                if (purged > 0) "CP08 ready • Purged $purged expired trash item(s)"
                else "CP08 Trash/Restore ready"
            textSize = 18f
        }
        root.addView(statusView)

        callStateView = TextView(this).apply {
            text = "Call state: monitor off"
            textSize = 16f
        }
        root.addView(callStateView)

        simView = TextView(this).apply {
            text = "SIMs: not loaded"
            textSize = 15f
        }
        root.addView(simView)

        libraryView = TextView(this).apply { textSize = 16f }
        root.addView(libraryView)

        root.addView(Button(this).apply {
            text = "Enable call-state monitor"
            setOnClickListener { requestOrStartCallMonitor() }
        })

        root.addView(Button(this).apply {
            text = "Refresh SIM inventory"
            setOnClickListener { requestOrRefreshSims() }
        })

        root.addView(Button(this).apply {
            text = "Enable automatic recording"
            setOnClickListener { enableAutoRecording() }
        })

        root.addView(Button(this).apply {
            text = "Disable automatic recording"
            setOnClickListener { disableAutoRecording() }
        })

        root.addView(Button(this).apply {
            text = "Start test recording"
            setOnClickListener { requestOrStartRecording() }
        })

        root.addView(Button(this).apply {
            text = "Stop recording"
            setOnClickListener { stopRecording() }
        })

        searchInput = EditText(this).apply {
            hint = "Search recordings, number or notes"
            setSingleLine(true)
        }
        root.addView(searchInput)

        root.addView(Button(this).apply {
            text = "Search"
            setOnClickListener {
                currentQuery = searchInput.text.toString()
                renderLibrary()
            }
        })

        modeButton = Button(this).apply {
            text = "Open Trash"
            setOnClickListener {
                showTrash = !showTrash
                text = if (showTrash) "Back to Library" else "Open Trash"
                renderLibrary()
            }
        }
        root.addView(modeButton)

        root.addView(Button(this).apply {
            text = "Purge expired Trash now"
            setOnClickListener {
                val count =
                    library.purgeExpiredTrash(retentionRepository.trashRetentionDays())
                statusView.text = "Purged $count expired trash item(s)"
                renderLibrary()
            }
        })
        root.addView(Button(this).apply {
            text = "Create encrypted portable backup"
            setOnClickListener { promptForBackupPassphrase() }
        })

        playbackView = TextView(this).apply {
            text = "Playback: idle"
            textSize = 16f
        }
        root.addView(playbackView)

        seekBar = SeekBar(this)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) player?.seekTo(progress)
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
        root.addView(seekBar)

        root.addView(Button(this).apply {
            text = "Stop playback"
            setOnClickListener { stopPlayback() }
        })

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(listContainer)

        setContentView(scroll)
        renderLibrary()

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            refreshSims()
        }
    }

    private fun promptForBackupPassphrase() {
        val input = EditText(this).apply {
            hint = "Backup passphrase (minimum 6 characters)"
            inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Encrypted portable backup")
            .setMessage("Keep this passphrase safe. It is required to restore the backup on another device.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val password = input.text.toString()
                if (password.length < 6) {
                    statusView.text = "Backup passphrase must be at least 6 characters"
                    return@setPositiveButton
                }

                statusView.text = "Creating encrypted backup..."
                Thread {
                    try {
                        val result =
                            backupManager.createPortableBackup(password.toCharArray())
                        runOnUiThread {
                            statusView.text =
                                "Encrypted backup: " + result.encryptedFiles +
                                    " files • " + result.directory
                        }
                    } catch (error: Throwable) {
                        runOnUiThread {
                            statusView.text =
                                "Backup failed: " +
                                    (error.message ?: error.javaClass.simpleName)
                        }
                    }
                }.start()
            }
            .show()
    }
    private fun renderLibrary() {
        val activeEntries =
            if (showTrash) emptyList() else library.search(currentQuery)
        val trashEntries = library.trash()
        val entries = if (showTrash) trashEntries else activeEntries

        libraryView.text =
            if (showTrash) {
                "Trash: " + trashEntries.size +
                    " • Auto-delete after " + retentionRepository.trashRetentionDays() + " days"
            } else {
                "Library: " + activeEntries.size + " shown • Trash: " + trashEntries.size
            }

        listContainer.removeAllViews()

        if (entries.isEmpty()) {
            listContainer.addView(TextView(this).apply {
                text = if (showTrash) "Trash is empty" else "No recordings"
                textSize = 16f
            })
            return
        }

        entries.forEach { entry ->
            if (showTrash) addTrashCard(entry) else addLibraryCard(entry)
        }
    }

    private fun addTrashCard(entry: RecordingEntry) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 24, 0, 24)
        }

        card.addView(TextView(this).apply {
            text = entry.displayName ?: File(entry.filePath).name
            textSize = 18f
        })

        card.addView(TextView(this).apply {
            text = "Moved to Trash: " + (entry.trashedAtEpochMs ?: 0L)
            textSize = 13f
        })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actions.addView(Button(this).apply {
            text = "Restore"
            setOnClickListener {
                library.restore(entry.id)
                statusView.text = "Recording restored"
                renderLibrary()
            }
        })

        actions.addView(Button(this).apply {
            text = "Delete forever"
            setOnClickListener {
                confirmPermanentDelete(entry)
            }
        })

        card.addView(actions)
        listContainer.addView(card)
    }

    private fun confirmPermanentDelete(entry: RecordingEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete recording permanently?")
            .setMessage("This removes the audio file and its library entry. It cannot be restored.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                library.deletePermanently(entry)
                statusView.text = "Recording permanently deleted"
                renderLibrary()
            }
            .show()
    }

    private fun addLibraryCard(entry: RecordingEntry) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 24, 0, 24)
        }

        card.addView(TextView(this).apply {
            val title = entry.displayName ?: File(entry.filePath).name
            val favorite = if (entry.favorite) " ★" else ""
            text = title + favorite
            textSize = 18f
        })

        card.addView(TextView(this).apply {
            val duration = entry.durationMs?.let(::formatMs) ?: "unknown"
            val sim = entry.simSubscriptionId?.let { " • SIM " + it } ?: ""
            text = "Duration: " + duration + sim + "\n" + entry.filePath
            textSize = 13f
        })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        actions.addView(Button(this).apply {
            text = "Play"
            setOnClickListener { play(entry) }
        })

        actions.addView(Button(this).apply {
            text = if (entry.favorite) "Unfavorite" else "Favorite"
            setOnClickListener {
                library.setFavorite(entry.id, !entry.favorite)
                renderLibrary()
            }
        })

        actions.addView(Button(this).apply {
            text = "Trash"
            setOnClickListener {
                if (playingEntry?.id == entry.id) stopPlayback()
                library.moveToTrash(entry.id)
                statusView.text = "Moved to Trash"
                renderLibrary()
            }
        })

        card.addView(actions)

        val noteEditor = EditText(this).apply {
            hint = "Note"
            setText(entry.note ?: "")
            setSingleLine(true)
        }
        card.addView(noteEditor)

        card.addView(Button(this).apply {
            text = "Save note"
            setOnClickListener {
                library.setNote(entry.id, noteEditor.text.toString())
                statusView.text = "Note saved"
                renderLibrary()
            }
        })

        listContainer.addView(card)
    }

    private fun enableAutoRecording() {
        val missing = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            missing += Manifest.permission.READ_PHONE_STATE
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing += Manifest.permission.RECORD_AUDIO
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            missing += Manifest.permission.POST_NOTIFICATIONS
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), REQUEST_AUTO_RECORDING)
            return
        }

        startAutoRecordingService()
    }

    private fun startAutoRecordingService() {
        rulesRepository.setEnabled(true)
        val intent = Intent(this, AutoRecordingService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        statusView.text = "Automatic recording enabled"
    }

    private fun disableAutoRecording() {
        rulesRepository.setEnabled(false)
        stopService(Intent(this, AutoRecordingService::class.java))
        statusView.text = "Automatic recording disabled"
    }

    private fun requestOrRefreshSims() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            pendingSimRefresh = true
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_PHONE_STATE)
            return
        }
        refreshSims()
    }

    private fun refreshSims() {
        val sims = simInventory.activeSubscriptions()
        simView.text =
            if (sims.isEmpty()) {
                "SIMs: unavailable or none active"
            } else {
                sims.joinToString(prefix = "SIMs: ", separator = " • ") { sim ->
                    "slot " + (sim.slotIndex + 1) + " " +
                        (sim.displayName ?: sim.carrierName ?: "SIM")
                }
            }
        pendingSimRefresh = false
    }

    private fun play(entry: RecordingEntry) {
        stopPlayback()

        val file = File(entry.filePath)
        if (!file.exists()) {
            statusView.text = "Missing audio file: " + file.name
            return
        }

        try {
            val newPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener { stopPlayback() }
                start()
            }
            player = newPlayer
            playingEntry = entry
            library.setDuration(entry.id, newPlayer.duration.toLong())
            playbackView.text = "Playing: " + (entry.displayName ?: file.name)
            seekBar.max = newPlayer.duration.coerceAtLeast(1)
            handler.post(progressUpdater)
        } catch (error: Throwable) {
            stopPlayback()
            statusView.text = "Playback failed: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun stopPlayback() {
        handler.removeCallbacks(progressUpdater)
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Throwable) {
            }
            it.release()
        }
        player = null
        playingEntry = null
        seekBar.progress = 0
        playbackView.text = "Playback: idle"
    }

    private fun requestOrStartCallMonitor() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_PHONE_STATE)
            return
        }
        startCallMonitor()
    }

    private fun startCallMonitor() {
        try {
            callMonitor.start()
            callStateView.text = "Call state: monitoring"
        } catch (error: Throwable) {
            callStateView.text = "Monitor failed: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun requestOrStartRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingStart = true
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }
        startRecording()
    }

    private fun startRecording() {
        if (recorder.state == RecorderState.RECORDING) {
            statusView.text = "Already recording"
            return
        }

        val baseDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
        val output = File(baseDir, "recordings/test_" + System.currentTimeMillis() + ".m4a")

        try {
            recorder.start(output)
            statusView.text = "Recording: " + output.name
        } catch (error: Throwable) {
            statusView.text = "Start failed: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun stopRecording() {
        try {
            val file = recorder.stop()
            if (file != null) {
                val entry = library.register(file)
                statusView.text = "Saved: " + entry.displayName
                renderLibrary()
            } else {
                statusView.text = "Recorder is idle"
            }
        } catch (error: Throwable) {
            statusView.text = "Stop failed: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_RECORD_AUDIO -> {
                val granted =
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (granted && pendingStart) startRecording()
                if (!granted) statusView.text = "Microphone permission is required"
                pendingStart = false
            }

            REQUEST_PHONE_STATE -> {
                val granted =
                    checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    if (pendingSimRefresh) refreshSims() else startCallMonitor()
                } else {
                    callStateView.text = "Phone-state permission is required"
                    simView.text = "SIMs: permission required"
                    pendingSimRefresh = false
                }
            }

            REQUEST_AUTO_RECORDING -> {
                val requiredGranted =
                    checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

                if (requiredGranted) {
                    startAutoRecordingService()
                } else {
                    statusView.text = "Phone and microphone permissions are required"
                }
            }
        }
    }

    override fun onDestroy() {
        stopPlayback()
        callMonitor.stop()

        if (recorder.state == RecorderState.RECORDING) {
            try {
                val file = recorder.stop()
                if (file != null) library.register(file)
            } catch (_: Throwable) {
                recorder.cancel()
            }
        }

        super.onDestroy()
    }

    private fun formatMs(value: Long): String {
        val totalSeconds = value / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return minutes.toString() + ":" + seconds.toString().padStart(2, '0')
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO = 40
        private const val REQUEST_PHONE_STATE = 41
        private const val REQUEST_AUTO_RECORDING = 42
    }
}

