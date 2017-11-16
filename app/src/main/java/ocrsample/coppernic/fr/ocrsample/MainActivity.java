package ocrsample.coppernic.fr.ocrsample;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.coppernic.sdk.ocr.MrzReader;
import fr.coppernic.sdk.power.PowerManager;
import fr.coppernic.sdk.power.api.PowerListener;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.utils.core.CpcDefinitions;
import fr.coppernic.sdk.utils.core.CpcResult;
import fr.coppernic.sdk.utils.io.InstanceListener;


public class MainActivity extends AppCompatActivity implements PowerListener, InstanceListener<MrzReader> {

    private static final String TAG = "MainActivity";
    // MRZ reader
    private MrzReader mrzReader;
    //UI
    private TextView tvOcr;
    private Context context;
    private Button btnFw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvOcr = (TextView)findViewById(R.id.tvOcr);

        btnFw = (Button)findViewById(R.id.btnVersion);
        btnFw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mrzReader.getFirmware();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearLogs();
            }
        });

        context = getApplicationContext();

        // Power management
        PowerManager.get().registerListener(this);
    }

    @Override
    protected void onStart() {
        // Powers on OCR reader
        ConePeripheral.OCR_ACCESSIS_AI310E_USB.on(context);
        super.onStart();
    }

    @Override
    protected void onStop() {
        mrzReader.close();
        addLogs(getString(R.string.reader_closed));
        // Powers off OCR reader
        ConePeripheral.OCR_ACCESSIS_AI310E_USB.off(context);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PowerManager.get().unregisterAll();
        PowerManager.get().releaseResources();
    }

    @Override
    public void onPowerUp(CpcResult.RESULT result, Peripheral peripheral) {
        if (result == CpcResult.RESULT.OK) {
            Log.d (TAG, "OCR reader powered on");
            // If OCR reader has been powered on
            MrzReader.Builder.get()
                .setListener(mrzListener)
                .withPort(CpcDefinitions.OCR_READER_PORT_CONE)
                .withBaudrate(CpcDefinitions.OCR_READER_BAUDRATE_CONE)
                .build(context,this);
        } else {
            addLogs(getString(R.string.ocr_powerup_failed));
        }
    }

    @Override
    public void onPowerDown(CpcResult.RESULT result, Peripheral peripheral) {
        enableUiAfterReaderInstantiation(false);
    }

    @Override
    public void onCreated(MrzReader mrzReader) {
        Log.d (TAG, getString(R.string.mrz_instance_created));
        this.mrzReader = mrzReader;
        if( this.mrzReader.open() == CpcResult.RESULT.OK) {
            addLogs(getString(R.string.reader_opened));
            enableUiAfterReaderInstantiation(true);
        }
        else{
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
    private void addLogs(String toLog){
        tvOcr.append("\n"+toLog);
    }

    /**
     * Clears log on screen
     */
    private void clearLogs(){
        tvOcr.setText("");
    }

    /**
     * Enables/disables UI after power state of RFID reader has been changed.
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
