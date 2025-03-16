package com.techsyllabi.voiceactivatedhomeautomation;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * MainActivity for Voice Activated Home Automation (VAHA) app.
 * This app lets you control home appliances (like lights, fans, TV, AC) using your voice or buttons.
 * It connects to a Bluetooth device (HC-05 module) to send commands to turn devices on or off.
 * You can use voice commands (e.g., "turn on light") or click buttons on the screen to control appliances.
 */
public class MainActivity extends AppCompatActivity {

    // Constants (values that don't change)
    private static final String TAG = "MainActivity"; // Tag for Logcat filtering
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Unique ID for HC-05 connection

    // Variables for Bluetooth and screen elements
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private TextView bluetoothStatusText;
    private TextView textOutput;
    private Button connectButton;
    private Button lightOn, lightOff;
    private Button fanOn, fanOff;
    private Button tvOn, tvOff;
    private Button acOn, acOff;
    private Button allOn, allOff;
    private ImageView connectImage;
    private ImageView lightOnByImg;
    private ImageView fanOnByImg;
    private ImageView tvOnByImg;
    private ImageView acOnByImg;

    // ActivityResultLaunchers for permissions and intents
    private final ActivityResultLauncher<String> requestMicPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "requestMicPermission: Microphone permission granted");
                    startSpeechRecognition();
                } else {
                    Log.w(TAG, "requestMicPermission: Microphone permission denied");
                    Toast.makeText(this, "Microphone permission denied. Please enable it in settings.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> requestBluetoothPermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Log.d(TAG, "requestBluetoothPermissions: All Bluetooth permissions granted");
                    checkBluetoothEnabled();
                } else {
                    Log.w(TAG, "requestBluetoothPermissions: Bluetooth permissions denied");
                    Toast.makeText(this, "Bluetooth permissions denied. Please enable them in Settings > Apps > VAHA App > Permissions.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> enableBluetooth =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.d(TAG, "enableBluetooth: Bluetooth enabled by user");
                    showPairedDevicesDialog();
                } else {
                    Log.w(TAG, "enableBluetooth: Bluetooth not enabled by user");
                    Toast.makeText(this, "Bluetooth not enabled. Please enable it in settings.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> speechRecognizer =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String spokenText = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                    Log.d(TAG, "speechRecognizer: Recognized text: " + spokenText);
                    textOutput.setText(spokenText);
                    processCommand(spokenText);
                } else {
                    Log.w(TAG, "speechRecognizer: Speech recognition failed or canceled");
                    Toast.makeText(this, "Speech recognition failed or canceled.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting VAHA activity");
        setContentView(R.layout.activity_main);

        // Initialize UI elements with null checks
        connectButton = findViewById(R.id.connectBluetoothButton);
        if (connectButton == null) Log.e(TAG, "onCreate: connectButton not found in layout");
        connectImage = findViewById(R.id.connectBluetoothIcon);
        if (connectImage == null) Log.e(TAG, "onCreate: connectImage not found in layout");
        bluetoothStatusText = findViewById(R.id.bluetoothStatus);
        if (bluetoothStatusText == null) Log.e(TAG, "onCreate: bluetoothStatusText not found in layout");
        textOutput = findViewById(R.id.textOutput);
        if (textOutput == null) Log.e(TAG, "onCreate: textOutput not found in layout");
        lightOn = findViewById(R.id.lightOnButton);
        if (lightOn == null) Log.e(TAG, "onCreate: lightOnButton not found in layout");
        lightOff = findViewById(R.id.lightOffButton);
        if (lightOff == null) Log.e(TAG, "onCreate: lightOffButton not found in layout");
        fanOn = findViewById(R.id.fanOnButton);
        if (fanOn == null) Log.e(TAG, "onCreate: fanOnButton not found in layout");
        fanOff = findViewById(R.id.fanOffButton);
        if (fanOff == null) Log.e(TAG, "onCreate: fanOffButton not found in layout");
        tvOn = findViewById(R.id.tvOnButton);
        if (tvOn == null) Log.e(TAG, "onCreate: tvOnButton not found in layout");
        tvOff = findViewById(R.id.tvOffButton);
        if (tvOff == null) Log.e(TAG, "onCreate: tvOffButton not found in layout");
        acOn = findViewById(R.id.acOnButton);
        if (acOn == null) Log.e(TAG, "onCreate: acOnButton not found in layout");
        acOff = findViewById(R.id.acOffButton);
        if (acOff == null) Log.e(TAG, "onCreate: acOffButton not found in layout");
        allOn = findViewById(R.id.allOnButton);
        if (allOn == null) Log.e(TAG, "onCreate: allOnButton not found in layout");
        allOff = findViewById(R.id.allOffButton);
        if (allOff == null) Log.e(TAG, "onCreate: allOffButton not found in layout");
        lightOnByImg = findViewById(R.id.lightOnImage);
        if (lightOnByImg == null) Log.e(TAG, "onCreate: lightOnImage not found in layout");
        fanOnByImg = findViewById(R.id.fanOnImage);
        if (fanOnByImg == null) Log.e(TAG, "onCreate: fanOnImage not found in layout");
        tvOnByImg = findViewById(R.id.tvOnImage);
        if (tvOnByImg == null) Log.e(TAG, "onCreate: tvOnImage not found in layout");
        acOnByImg = findViewById(R.id.acOnImage);
        if (acOnByImg == null) Log.e(TAG, "onCreate: acOnImage not found in layout");

        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "onCreate: Bluetooth not supported on this device");
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.d(TAG, "onCreate: Bluetooth adapter initialized");

        // Set click listeners with debugging
        connectButton.setOnClickListener(v -> {
            Log.d(TAG, "connectButton: Clicked to initiate Bluetooth pairing");
            handleBluetoothPairing();
        });
        connectImage.setOnClickListener(v -> {
            Log.d(TAG, "connectImage: Clicked to initiate Bluetooth pairing");
            handleBluetoothPairing();
        });
        lightOn.setOnClickListener(v -> {
            Log.d(TAG, "lightOn: Button clicked, sending TURN ON LIGHT#");
            sendViaBluetoothWithPermissionCheck("TURN ON LIGHT#");
        });
        lightOff.setOnClickListener(v -> {
            Log.d(TAG, "lightOff: Button clicked, sending TURN OFF LIGHT#");
            sendViaBluetoothWithPermissionCheck("TURN OFF LIGHT#");
        });
        fanOn.setOnClickListener(v -> {
            Log.d(TAG, "fanOn: Button clicked, sending TURN ON FAN#");
            sendViaBluetoothWithPermissionCheck("TURN ON FAN#");
        });
        fanOff.setOnClickListener(v -> {
            Log.d(TAG, "fanOff: Button clicked, sending TURN OFF FAN#");
            sendViaBluetoothWithPermissionCheck("TURN OFF FAN#");
        });
        tvOn.setOnClickListener(v -> {
            Log.d(TAG, "tvOn: Button clicked, sending TURN ON TV#");
            sendViaBluetoothWithPermissionCheck("TURN ON TV#");
        });
        tvOff.setOnClickListener(v -> {
            Log.d(TAG, "tvOff: Button clicked, sending TURN OFF TV#");
            sendViaBluetoothWithPermissionCheck("TURN OFF TV#");
        });
        acOn.setOnClickListener(v -> {
            Log.d(TAG, "acOn: Button clicked, sending TURN ON AC#");
            sendViaBluetoothWithPermissionCheck("TURN ON AC#");
        });
        acOff.setOnClickListener(v -> {
            Log.d(TAG, "acOff: Button clicked, sending TURN OFF AC#");
            sendViaBluetoothWithPermissionCheck("TURN OFF AC#");
        });
        allOn.setOnClickListener(v -> {
            Log.d(TAG, "allOn: Button clicked, sending TURN ON ALL DEVICES#");
            sendViaBluetoothWithPermissionCheck("TURN ON ALL DEVICES#");
        });
        allOff.setOnClickListener(v -> {
            Log.d(TAG, "allOff: Button clicked, sending TURN OFF ALL DEVICES#");
            sendViaBluetoothWithPermissionCheck("TURN OFF ALL DEVICES#");
        });
        lightOnByImg.setOnClickListener(v -> {
            Log.d(TAG, "lightOnByImg: Image clicked, sending TURN ON LIGHT#");
            sendViaBluetoothWithPermissionCheck("TURN ON LIGHT#");
        });
        fanOnByImg.setOnClickListener(v -> {
            Log.d(TAG, "fanOnByImg: Image clicked, sending TURN ON FAN#");
            sendViaBluetoothWithPermissionCheck("TURN ON FAN#");
        });
        tvOnByImg.setOnClickListener(v -> {
            Log.d(TAG, "tvOnByImg: Image clicked, sending TURN ON TV#");
            sendViaBluetoothWithPermissionCheck("TURN ON TV#");
        });
        acOnByImg.setOnClickListener(v -> {
            Log.d(TAG, "acOnByImg: Image clicked, sending TURN ON AC#");
            sendViaBluetoothWithPermissionCheck("TURN ON AC#");
        });
    }

    public void onMicClick(View view) {
        Log.d(TAG, "onMicClick: Microphone button clicked");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onMicClick: Microphone permission granted, starting recognition");
            startSpeechRecognition();
        } else {
            Log.w(TAG, "onMicClick: Microphone permission denied, requesting");
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void handleBluetoothPairing() {
        Log.d(TAG, "handleBluetoothPairing: Starting Bluetooth pairing process");
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
        } else {
            permissions = new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        }

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        if (allPermissionsGranted) {
            Log.d(TAG, "handleBluetoothPairing: All permissions granted, checking Bluetooth");
            checkBluetoothEnabled();
        } else {
            Log.w(TAG, "handleBluetoothPairing: Permissions not granted, requesting");
            requestBluetoothPermissions.launch(permissions);
        }
    }

    private void checkBluetoothEnabled() {
        Log.d(TAG, "checkBluetoothEnabled: Checking Bluetooth status");
        if (!bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "checkBluetoothEnabled: Bluetooth disabled, requesting enable");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetooth.launch(enableBtIntent);
        } else {
            Log.d(TAG, "checkBluetoothEnabled: Bluetooth enabled, showing paired devices");
            showPairedDevicesDialog();
        }
    }

    private void showPairedDevicesDialog() {
        Log.d(TAG, "showPairedDevicesDialog: Displaying paired devices");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "showPairedDevicesDialog: Bluetooth permissions missing, requesting");
                Toast.makeText(this, "Bluetooth permissions required. Requesting again...", Toast.LENGTH_SHORT).show();
                requestBluetoothPermissions.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "showPairedDevicesDialog: Bluetooth permissions missing, requesting");
                Toast.makeText(this, "Bluetooth permissions required. Requesting again...", Toast.LENGTH_SHORT).show();
                requestBluetoothPermissions.launch(new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN});
                return;
            }
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            Log.w(TAG, "showPairedDevicesDialog: No paired devices found");
            bluetoothStatusText.setText("No paired devices. Pair one in Settings.");
            return;
        }
        Log.d(TAG, "showPairedDevicesDialog: Found " + pairedDevices.size() + " paired devices");

        ArrayList<String> deviceNames = new ArrayList<>();
        ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            deviceNames.add(device.getName() + " (" + device.getAddress() + ")");
            devicesList.add(device);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Bluetooth Device")
                .setItems(deviceNames.toArray(new CharSequence[0]), (dialog, which) -> {
                    Log.d(TAG, "showPairedDevicesDialog: User selected device: " + deviceNames.get(which));
                    connectToDevice(devicesList.get(which));
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Log.d(TAG, "showPairedDevicesDialog: User canceled device selection");
                    dialog.dismiss();
                })
                .setCancelable(false)
                .create()
                .show();
    }

    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.d(TAG, "connectToDevice: Attempting to connect to " + device.getName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "connectToDevice: Bluetooth_CONNECT permission denied");
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "connectToDevice: Bluetooth permission denied");
                return;
            }
        }

        bluetoothStatusText.setText("Connecting to " + device.getName());
        new Thread(() -> {
            BluetoothSocket tempSocket = null;
            try {
                Log.d(TAG, "connectToDevice: Creating RFCOMM socket");
                tempSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d(TAG, "connectToDevice: Attempting connection");
                tempSocket.connect();
                Log.d(TAG, "connectToDevice: Connection established");
                outputStream = tempSocket.getOutputStream();
                bluetoothSocket = tempSocket;

                runOnUiThread(() -> {
                    Log.d(TAG, "connectToDevice: Updating UI with connection status");
                    bluetoothStatusText.setText("Bluetooth Connected to " + device.getName());
                    bluetoothStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                });
            } catch (IOException | SecurityException e) {
                Log.e(TAG, "connectToDevice: Connection failed - " + e.getMessage(), e);
                runOnUiThread(() -> {
                    bluetoothStatusText.setText("Connection Failed: " + e.getMessage());
                    bluetoothStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                });
                if (tempSocket != null) {
                    try {
                        Log.d(TAG, "connectToDevice: Closing failed socket");
                        tempSocket.close();
                    } catch (IOException closeException) {
                        Log.e(TAG, "connectToDevice: Failed to close socket - " + closeException.getMessage(), closeException);
                    }
                }
            }
        }).start();
    }

    private void startSpeechRecognition() {
        Log.d(TAG, "startSpeechRecognition: Initiating speech recognition");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            Log.d(TAG, "startSpeechRecognition: Launching speech recognizer");
            speechRecognizer.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "startSpeechRecognition: Error launching speech recognition - " + e.getMessage(), e);
            Toast.makeText(this, "Speech recognition not available. Ensure Google app is installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void sendViaBluetoothWithPermissionCheck(String text) {
        Log.d(TAG, "sendViaBluetoothWithPermissionCheck: Processing command: " + text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                sendViaBluetooth(text);
            } else {
                Log.w(TAG, "sendViaBluetoothWithPermissionCheck: Bluetooth_CONNECT permission denied");
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                requestBluetoothPermissions.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                sendViaBluetooth(text);
            } else {
                Log.w(TAG, "sendViaBluetoothWithPermissionCheck: Bluetooth permission denied");
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                requestBluetoothPermissions.launch(new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN});
            }
        }
    }

    private void sendViaBluetooth(String text) {
        Log.d(TAG, "sendViaBluetooth: Attempting to send: " + text);
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            new Thread(() -> {
                try {
                    Log.d(TAG, "sendViaBluetooth: Writing command to output stream");
                    outputStream.write((text + "\n").getBytes());
                    outputStream.flush();
                    Log.d(TAG, "sendViaBluetooth: Command sent successfully");
                    runOnUiThread(() -> Toast.makeText(this, "Sent: " + text, Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    Log.e(TAG, "sendViaBluetooth: Failed to send - " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            Log.w(TAG, "sendViaBluetooth: Bluetooth not connected or socket null");
            runOnUiThread(() -> Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show());
        }
    }

    private void processCommand(String command) {
        Log.d(TAG, "processCommand: Received raw command: " + command);
        String commandUpper = command.toUpperCase().trim();
        String commandWithDelimiter = commandUpper + "#";
        Log.d(TAG, "processCommand: Processed command: " + commandWithDelimiter);

        switch (commandUpper) {
            case "TURN ON LIGHT":
            case "LIGHT ON":
            case "LIGHT":
            case "LIGHT CHALU":
                Log.d(TAG, "processCommand: Executing TURN ON LIGHT");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            case "TURN OFF LIGHT":
            case "LIGHT OFF":
            case "LIGHT BAND":
                Log.d(TAG, "processCommand: Executing TURN OFF LIGHT");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            case "TURN ON FAN":
            case "FAN ON":
            case "FAN":
            case "PANKHA CHALU":
                Log.d(TAG, "processCommand: Executing TURN ON FAN");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            case "TURN OFF FAN":
            case "FAN OFF":
            case "PANKHA BAND":
                Log.d(TAG, "processCommand: Executing TURN OFF FAN");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            case "TURN ON TV":
            case "TV ON":
            case "TV":
            case "TV CHALU":
                Log.d(TAG, "processCommand: Executing TURN ON TV");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            case "TURN OFF TV":
            case "TV OFF":
            case "TV BAND":
                Log.d(TAG, "processCommand: Executing TURN OFF TV");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            case "TURN ON AC":
            case "AC ON":
            case "AC":
            case "AC CHALU":
                Log.d(TAG, "processCommand: Executing TURN ON AC");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            case "TURN OFF AC":
            case "AC OFF":
            case "AC BAND":
                Log.d(TAG, "processCommand: Executing TURN OFF AC");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            case "TURN ON ALL DEVICES":
            case "SAB CHALU":
                Log.d(TAG, "processCommand: Executing TURN ON ALL DEVICES");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            case "TURN OFF ALL DEVICES":
            case "SAB BAND":
                Log.d(TAG, "processCommand: Executing TURN OFF ALL DEVICES");
                sendViaBluetoothWithPermissionCheck(commandWithDelimiter);
                break;
            default:
                Log.w(TAG, "processCommand: Unrecognized command: " + commandUpper);
                Toast.makeText(this, "Command not recognized", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Cleaning up resources");
        try {
            if (bluetoothSocket != null) {
                Log.d(TAG, "onDestroy: Closing Bluetooth socket");
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "onDestroy: Failed to close socket - " + e.getMessage(), e);
        }
    }
}

package com.techsyllabi.voiceactivatedhomeautomation;

import android.util.Log;
// ... (other imports)

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity"; // Tag for Logcat filtering

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: VAHA app started"); // Confirm app start

        // Initialize UI elements
        connectButton = findViewById(R.id.connectBluetoothButton);
        connectImage = findViewById(R.id.connectBluetoothIcon);
        bluetoothStatusText = findViewById(R.id.bluetoothStatus);
        textOutput = findViewById(R.id.textOutput);
        // ... (other initializations)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "onCreate: Bluetooth not supported on this device");
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.d(TAG, "onCreate: Bluetooth adapter initialized successfully");

        // Add logs to click listeners
        connectButton.setOnClickListener(v -> {
            Log.d(TAG, "connectButton: Clicked to initiate Bluetooth pairing");
            handleBluetoothPairing();
        });
        connectImage.setOnClickListener(v -> {
            Log.d(TAG, "connectImage: Clicked to initiate Bluetooth pairing");
            handleBluetoothPairing();
        });
        lightOn.setOnClickListener(v -> {
            Log.d(TAG, "lightOn: Button clicked, sending TURN ON LIGHT#");
            sendViaBluetoothWithPermissionCheck("TURN ON LIGHT#");
        });
        // ... (similar logs for other buttons like fanOn, tvOn, etc.)
    }

    public void onMicClick(View view) {
        Log.d(TAG, "onMicClick: Microphone button clicked");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onMicClick: Microphone permission granted, starting recognition");
            startSpeechRecognition();
        } else {
            Log.w(TAG, "onMicClick: Microphone permission denied, requesting permission");
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void handleBluetoothPairing() {
        Log.d(TAG, "handleBluetoothPairing: Starting Bluetooth pairing process");
        // ... (existing code)
    }

    private void startSpeechRecognition() {
        Log.d(TAG, "startSpeechRecognition: Initiating speech recognition");
        // ... (existing code)
    }

    private void processCommand(String command) {
        Log.d(TAG, "processCommand: Received command: " + command);
        // ... (existing code)
    }

    private void sendViaBluetooth(String text) {
        Log.d(TAG, "sendViaBluetooth: Attempting to send: " + text);
        // ... (existing code)
    }
}