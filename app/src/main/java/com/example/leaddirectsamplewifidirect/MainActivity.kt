package com.example.leaddirectsamplewifidirect

import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.leaddirectsamplewifidirect.adapters.DeviceListAdapter
import com.example.leaddirectsamplewifidirect.databinding.ActivityMainBinding
import com.example.leaddirectsamplewifidirect.filechooserhelper.*
import com.example.leaddirectsamplewifidirect.utils.PermissionsHelper
import com.example.leadp2p.p2p.LeadP2PHandler
import com.example.leadp2p.p2p.PeerDevice
import com.example.leadp2pdirect.P2PCallBacks
import com.example.leadp2pdirect.chatmessages.ChatCommands
import com.example.leadp2pdirect.chatmessages.ResourceSyncCommand
import com.example.leadp2pdirect.chatmessages.VideoCommands
import com.example.leadp2pdirect.chatmessages.constants.ChatType
import com.example.leadp2pdirect.chatmessages.enumss.TransferStatus
import com.example.leadp2pdirect.chatmessages.enumss.VideoPlayBacks
import com.example.leadp2pdirect.helpers.Utils
import com.example.leadp2pdirect.p2p.MyDeviceInfoForQrCode
import com.example.leadp2pdirect.servers.FileDownloadUploadProgresssModel
import com.example.leadp2pdirect.servers.FileHelper.mimeType
import com.example.leadp2pdirect.servers.FileModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.File


class MainActivity : AppCompatActivity(), P2PCallBacks, OnRecyclerViewItemClick {
    private var doubleBackToExitPressedOnce = false
    private lateinit var binding: ActivityMainBinding
    private var leadp2pHander: LeadP2PHandler? = null
    private var deviceListAdapter: DeviceListAdapter? = null
    private var peersList = ArrayList<PeerDevice>()

    private var selectedDevice: PeerDevice? = null
    private var progressDialog: ProgressDialog? = null

    private var isSyncResources = false;
    private var uriList: ArrayList<Uri>? = null

    private fun initializeWifiDirectSdk() {
        leadp2pHander = LeadP2PHandler(this, this, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PermissionsHelper.isReadStorageAndLocationPermissionGranted(this)) {
            initializeWifiDirectSdk()
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceListAdapter = DeviceListAdapter(peersList, this)
        binding.peerRecyclerView.setHasFixedSize(true)
        binding.peerRecyclerView.setLayoutManager(LinearLayoutManager(this))
        binding.peerRecyclerView.setAdapter(deviceListAdapter)

        setOnClickListeners()
        setWifiButton()
    }


    fun tempfun(filename: String?, videoPlayBacks: VideoPlayBacks): ChatCommands {
        val videoCommands = VideoCommands(videoPlayBacks, filename!!)
        return ChatCommands(
            ChatType.VIDEO_CHAT_TYPE,
            TransferStatus.SENT,
            videoCommands,
            null
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu);
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_scan -> {
                QRScanner.scan(this)
                true
            }
            R.id.action_disconnect -> {
                val builder = AlertDialog.Builder(this)
                with(builder)
                {
                    setTitle("Disconnect Device")
                    setMessage("Are you sure, you want to disconnect?")
                    setPositiveButton("OK") { dialoginterface, which ->
                        leadp2pHander?.disconnect(selectedDevice)
                    }
                    setNegativeButton(android.R.string.no) { dialoginterface, which ->
                    }
                    show()
                }
                true
            }

            R.id.action_sync -> {
                isSyncResources = true;
                ChooseFile.fileChooser(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Utils.showToast(this, "Please click BACK again to exit")

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

    private suspend fun syncResources(uris: ArrayList<Uri>) {
        uriList = uris
        val syncChatData = GlobalScope.async(Dispatchers.IO) {
            getFileModelsListFromUris(uris)
        }.await()

        val resourceSyncChatCommand = ChatCommands(
            ChatType.RESOURCE_SYNC_CHAT_TYPE,
            TransferStatus.SENT,
            resourceSyncCommand = ResourceSyncCommand(syncChatData, false)
        )
        val info = Gson().toJson(resourceSyncChatCommand)
        leadp2pHander?.sendMessage(info.toString(), selectedDevice)
        if (syncChatData != null) {
            Log.d("sychChatDAta", syncChatData)
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("permisssionsss", "Permission: " + permissions[0] + "was " + grantResults[0])
            //resume tasks needing this permission
            initializeWifiDirectSdk()
        }
    }

    private fun initializeProgressDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog?.setMax(100)
        progressDialog?.setMessage("Loading....")
        progressDialog?.setProgressNumberFormat(null)
        progressDialog?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    }

    fun setOnClickListeners() {
        binding.buttonPlayPause.setOnClickListener(View.OnClickListener {
            val text: String = binding.buttonPlayPause.text.toString()
            if (text.equals("Play", ignoreCase = true)) {
                binding.buttonPlayPause.text = "Pause"
                val tempChatCommands = tempfun("acss", VideoPlayBacks.PLAY)
                val info = Gson().toJson(tempChatCommands)
                leadp2pHander?.sendMessage(info.toString(), selectedDevice)
            } else {
                binding.buttonPlayPause.text = "Play"
                val tempChatCommands = tempfun("acss", VideoPlayBacks.PAUSE)
                val info = Gson().toJson(tempChatCommands)
                leadp2pHander?.sendMessage(info.toString(), selectedDevice)
            }
        })


        binding.wifiBtn.setOnClickListener(View.OnClickListener {
            leadp2pHander?.turnWifiOnOff()
        })

        binding.buttonShare.setOnClickListener(View.OnClickListener {
            selectedDevice.let {
                isSyncResources = false
                ChooseFile.fileChooser(this)
            }
        })

        binding.buttonChat.setOnClickListener(View.OnClickListener {
            selectedDevice.let {
                leadp2pHander?.sendMessage("hello", it)
            }
        })
    }

    fun setWifiButton() {
        if (leadp2pHander?.isWifiEnabled() == true) {
            binding.wifiBtn.setText("Wifi Off")
        } else {
            binding.wifiBtn.setText("Wifi On")
        }
    }

    fun showAndHideUIIfConnected() {
        if (selectedDevice == null) {
            setTitle(getString(R.string.app_name))
            binding.wifiBtn.visibility = View.VISIBLE
            binding.peerRecyclerView.visibility = View.VISIBLE
        } else {
            setTitle("Connected To :- ${selectedDevice?.deviceName}")
            binding.wifiBtn.visibility = View.GONE
            binding.peerRecyclerView.visibility = View.GONE
        }
    }


    /* register the broadcast receiver with the intent values to be matched */
    override fun onResume() {
        super.onResume()
        leadp2pHander?.registerReceiver()
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        leadp2pHander?.unRegisterReceiver()
    }

    //............... Library method callbacks.............................

    override fun onProgressUpdate(fileDownloadProgresssModel: FileDownloadUploadProgresssModel) {
        runOnUiThread(Runnable {
            Log.d(
                "Progress....",
                "${fileDownloadProgresssModel.fileName} --  ${fileDownloadProgresssModel.progressPercentage}%"
            )
            if (progressDialog == null) {
                initializeProgressDialog()
            }
            if (progressDialog?.isShowing == false) {
                progressDialog?.show()
                progressDialog?.setMessage("${fileDownloadProgresssModel.sendingOrReceiving}...\n${fileDownloadProgresssModel.fileName}")
            }
            progressDialog?.progress = fileDownloadProgresssModel.progressPercentage
            if (fileDownloadProgresssModel.progressPercentage == 100) {
                progressDialog?.cancel()
            }
        })
    }

    override fun onFilesReceived(filePathsList: ArrayList<FileModel>) {
        Utils.showToast(this, "File Received.....")

        /* val intent = Intent(this, MediaActivity::class.java)
         intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
         intent.putStringArrayListExtra("filePathsList", filePathsList.get(0).absoluteFilePath);
         startActivity(intent)*/
    }


    override fun onConnected(peerDevice: PeerDevice?) {
        selectedDevice = peerDevice
        showAndHideUIIfConnected()
    }

    override fun onDisconnected() {
        leadp2pHander?.startPeersDiscovery()
    }


    override fun updatePeersList(listPeerDevice: ArrayList<PeerDevice>?) {
        peersList?.clear()
        if (listPeerDevice != null) {
            peersList?.addAll(listPeerDevice)
            deviceListAdapter?.notifyDataSetChanged()
        }
    }

    override fun timeTakenByFileToSend(message: String) {
        runOnUiThread(
            Runnable {
                binding.timeTakenTV.text = message
            }
        )
    }

    override fun sendLogBackToSender(logMesaage: String) {
    }

    override fun myDeviceInfo(deviceInfoForQrCode: String) {
        if (leadp2pHander?.isConnected() == false) {
            peersList.clear()
            selectedDevice = null
            leadp2pHander?.startPeersDiscovery()
            showAndHideUIIfConnected()
        }
    }

    override fun onReceiveChatCommands(chatCommands: ChatCommands) {
        if (chatCommands != null) {
            when (chatCommands.chatType) {
                ChatType.RESOURCE_SYNC_CHAT_TYPE -> {
                    val resourceSyncCommand = chatCommands.resourceSyncCommand?.resourceData
                    val myType = object : TypeToken<ArrayList<FileModel>>() {}.type
                    val fileModelArraylist =
                        Gson().fromJson<ArrayList<FileModel>>(resourceSyncCommand, myType)
                    if (chatCommands.transferStatus == TransferStatus.RECEIVED &&
                        chatCommands.resourceSyncCommand?.isSynced == true
                    ) {
                        uriList?.let { leadp2pHander?.transferFile(it) }
                    }
                }
            }
        }
    }

//............... End Library method callbacks.............................


    override fun onItemClick(position: Int) {
        peersList.get(position).let {
            leadp2pHander?.connect(it)
        }
    }


    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ChooseFile.FILE_TRANSFER_CODE -> {
                if (data == null) return
                val uris = ArrayList<Uri>()
                val filesLength = ArrayList<Long>()
                val fileNames = ArrayList<String>()

                try {
                    val clipData = data.clipData
                    if (clipData != null) {
                        var i = 0
                        while (i < clipData.itemCount) {
                            uris.add(clipData.getItemAt(i).uri)
                            var fileName: String
                            try {
                                fileName = RealPathUtilJava.getRealPath(
                                    applicationContext,
                                    clipData.getItemAt(i).uri
                                )
                            } catch (ex: java.lang.Exception) {
                                fileName =
                                    FileUtils.getPath(applicationContext, clipData.getItemAt(i).uri)
                            }
                            if (fileName == null) {
                                fileName = MediaUtility.getPath(
                                    applicationContext,
                                    clipData.getItemAt(i).uri
                                );
                            }
                            filesLength.add(File(fileName).length())
                            fileName = FilesUtil.getFileName(fileName)
                            fileNames.add(fileName)
                            Log.d("File URI", clipData.getItemAt(i).uri.toString())
                            Log.d("File Path", fileName)
                            i++
                        }
                    } else {
                        val uri = data.data
                        if (uri != null) {
                            uris.add(uri)
                        }
                        var fileName: String
                        try {
                            fileName = RealPathUtilJava.getRealPath(applicationContext, uri)
                        } catch (ex: java.lang.Exception) {
                            fileName = FileUtils.getPath(applicationContext, uri)
                        }
                        if (fileName == null) {
                            fileName = MediaUtility.getPath(applicationContext, uri);
                        }
                        Log.d("File Path", "MediaUtility--- >   " + fileName)

                        filesLength.add(File(fileName).length())
                        fileName = FilesUtil.getFileName(fileName)
                        fileNames.add(fileName)
                    }

                    if (isSyncResources) {
                        runBlocking { syncResources(uris) }
                    } else {
                        leadp2pHander?.transferFile(uris)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            QRScanner.QRSCANNER_CODE -> {
                val result: String? =
                    data?.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult")
                if (result != null) {
                    Log.d("myscannnn", result)
                }
                val myDeviceInfoForQrCode =
                    Gson().fromJson(result, MyDeviceInfoForQrCode::class.java)
                val wifiP2pDevice = WifiP2pDevice()
                wifiP2pDevice.deviceName = myDeviceInfoForQrCode.deviceName
                wifiP2pDevice.deviceAddress = myDeviceInfoForQrCode.deviceId
                wifiP2pDevice.status = myDeviceInfoForQrCode.status
                wifiP2pDevice.primaryDeviceType = myDeviceInfoForQrCode.primaryDeviceType
                wifiP2pDevice.secondaryDeviceType = myDeviceInfoForQrCode.secondaryDeviceType
                val peerDevice = PeerDevice(
                    wifiP2pDevice.deviceName,
                    wifiP2pDevice.deviceAddress,
                    wifiP2pDevice.status,
                    wifiP2pDevice
                )
                leadp2pHander?.connect(peerDevice)
            }
        }
    }


    private fun getFileModelsListFromUris(uris: ArrayList<Uri>): String? {
        val cr = contentResolver
        val fileModelArrayList = java.util.ArrayList<FileModel>()
        for (i in uris.indices) {
            val returnUri = uris[i]
            val returnCursor: Cursor? = returnUri?.let { cr.query(it, null, null, null, null) }
            val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = returnCursor?.getColumnIndex(OpenableColumns.SIZE)
            returnCursor?.moveToFirst()
            val fileName = nameIndex?.let { returnCursor?.getString(it) }
            val fileLength = sizeIndex?.let { returnCursor?.getLong(it) }
            val mimeType = returnUri?.let { mimeType(it, cr) }
            Log.d("fileinfoooooo", mimeType!!)

            val fileModel = FileModel()
            fileModel.id = System.currentTimeMillis()
            fileModel.fileName = fileName
            fileModel.fileLength = fileLength
            fileModel.mimeType = mimeType
            if (mimeType != null) {
                if (mimeType.contains("image")) {
                    fileModel.type = FileModel.TYPE_PHOTO
                } else if (mimeType.contains("video")) {
                    fileModel.type = FileModel.TYPE_VIDEO
                } else if (mimeType.contains("pdf")) {
                    fileModel.type = FileModel.TYPE_PDF
                } else if (mimeType.contains("application/vnd.android.package-archive")) {
                    fileModel.type = FileModel.TYPE_APPLICATION
                } else if (mimeType.contains("application/zip")) {
                    fileModel.type = FileModel.TYPE_ZIP
                }
            }
            fileModelArrayList.add(fileModel)
            returnCursor?.close()
        }
        return Gson().toJson(fileModelArrayList).toString()
    }
}