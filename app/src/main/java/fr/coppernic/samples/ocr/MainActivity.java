package fr.coppernic.samples.ocr;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.coppernic.sdk.ocr.MrzReader;
import fr.coppernic.sdk.power.PowerManager;
import fr.coppernic.sdk.power.api.PowerListener;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.core.Defines;
import fr.coppernic.sdk.power.impl.idplatform.IdPlatformPeripheral;
import fr.coppernic.sdk.utils.core.CpcResult;
import fr.coppernic.sdk.utils.helpers.OsHelper;
import fr.coppernic.sdk.utils.io.InstanceListener;
import ocrsample.coppernic.fr.ocrsample.R;


public class MainActivity extends AppCompatActivity implements PowerListener, InstanceListener<MrzReader> {

    private static final String TAG = "MainActivity";
    // MRZ reader
    private MrzReader mrzReader;
    //UI
    private TextView tvOcr;
    private Button btnFw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvOcr = findViewById(R.id.tvOcr);

        btnFw = findViewById(R.id.btnVersion);
        btnFw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mrzReader != null) {
                    mrzReader.getFirmware();
                } else {
                    addLogs(getString(R.string.error_no_reader));
                }
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearLogs();
            }
        });

        // Power management
        PowerManager.get().registerListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Powers on OCR reader
        if(OsHelper.isCone())
            ConePeripheral.OCR_ACCESSIS_AI310E_USB.on(this);
        else if(OsHelper.isIdPlatform())
            IdPlatformPeripheral.OCR.on(this);
    }

    @Override
    protected void onStop() {
        if (mrzReader != null) {
            mrzReader.close();
        }
        addLogs(getString(R.string.reader_closed));
        // Powers off OCR reader
        if(OsHelper.isCone()) {
            ConePeripheral.OCR_ACCESSIS_AI310E_USB.off(this);
            ConePeripheral.USB_HOST_ASKEY_CONE_GPIO.off(this);
        } else if (OsHelper.isIdPlatform()){
           // IdPlatformPeripheral.OCR.off(this);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        PowerManager.get().unregisterAll();
        PowerManager.get().releaseResources();
        super.onDestroy();
    }

    @Override
    public void onPowerUp(CpcResult.RESULT result, Peripheral peripheral) {
        if (result == CpcResult.RESULT.OK) {
            Log.d(TAG, getString(R.string.reader_powered_on));
            // If OCR reader has been powered on
            if(OsHelper.isCone()) {
                MrzReader.Builder.get()
                        .setListener(mrzListener)
                        .withPort(Defines.SerialDefines.OCR_READER_PORT_CONE)
                        .withBaudrate(Defines.SerialDefines.OCR_READER_BAUDRATE_CONE)
                        .build(this, this);
            } else if (OsHelper.isIdPlatform()){
                addLogs(getString(R.string.reader_opened));
                enableUiAfterReaderInstantiation(true);
            }

        } else {
            addLogs(getString(R.string.ocr_powerup_failed, result));
        }
    }

    @Override
    public void onPowerDown(CpcResult.RESULT result, Peripheral peripheral) {
        enableUiAfterReaderInstantiation(false);
    }

    @Override
    public void onCreated(MrzReader mrzReader) {
        Log.d(TAG, getString(R.string.mrz_instance_created));
        this.mrzReader = mrzReader;
        if (this.mrzReader.open() == CpcResult.RESULT.OK) {
            addLogs(getString(R.string.reader_opened));
            enableUiAfterReaderInstantiation(true);
        } else {
            addLogs(getString(R.string.error_opening_reader));
        }
    }

    @Override
    public void onDisposed(MrzReader mrzReader) {

    }

    private final MrzReader.Listener mrzListener = new MrzReader.Listener() {
        @Override
        public void onFirmware(String firmware) {
            addLogs(firmware);
        }

        @Override
        public void onMenuData(String menu) {
            addLogs(menu);
        }

        @Override
        public void onMrz(String mrz) {
            addLogs(mrz);
        }
    };

    /***
     * Displays log on screen
     * @param toLog data to display
     */
    private void addLogs(String toLog) {
        tvOcr.append("\n" + toLog);
    }

    /**
     * Clears log on screen
     */
    private void clearLogs() {
        tvOcr.setText("");
    }

    /**
     * Enables/disables UI after power state of RFID reader has been changed.
     *
     * @param enable true: enables, false: disables
     */
    private void enableUiAfterReaderInstantiation(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnFw.setEnabled(enable);
            }
        });
    }
}
