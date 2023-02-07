# OcrSample
Sample application for Ocr reader on C-One e-ID

## Prerequisites

CoreServices shall be installed on your device.
Please install the last version available on FDroid already installed
 on your device.
 
 ## Set up

### build.gradle

```groovy
repositories {
    jcenter()
    maven { url "https://nexus.coppernic.fr/repository/libs-release" }
}


dependencies {
// [...]
    compile 'fr.coppernic.sdk.core:CpcCore:2.0.0'
    compile 'fr.coppernic.sdk.ocr:CpcOcr:3.0.0'
    compile 'fr.coppernic.sdk.cpcutils:CpcUtilsLib:6.17.2'
// [...]
}

```

### Power management

 * Implements power listener

```java

public class MainActivity extends AppCompatActivity implements PowerListener, InstanceListener<MrzReader> {
  // [...]
  @Override
  public void onPowerUp(RESULT res, Peripheral peripheral) {
    if (res == RESULT.OK) {
      //Peripheral is on
    } else {
      //Peripehral is undefined
    }
  }

  @Override
  public void onPowerDown(RESULT res, Peripheral peripheral) {
      //Peripehral is off
  }
  // [...]
}

```

 * Register the listener

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    PowerManager.get().registerListener(this);
}
```

 * Power reader on

```java
// Powers on OCR reader
ConePeripheral.OCR_ACCESSIS_AI310E_USB.on(context);
// The listener will be called with the result
```

 * Power off when you are done

```java
// Powers off OCR reader
ConePeripheral.OCR_ACCESSIS_AI310E_USB.off(context);
// The listener will be called with the result
```

 * release resources

```java
@Override
protected void onDestroy() {
    PowerManager.get().unregisterAll();
    PowerManager.get().releaseResources();
    super.onDestroy();
}
```

### Reader initialization

#### Create reader object
 * Declare a Reader object

```java
private MrzReader mrzReader;
```
 * Create a listener 
 
```java
private final MrzReader.Listener mrzListener = new MrzReader.Listener() {
        @Override
        public void onFirmware(String firmware) {
            //Display Firmware version
        }

        @Override
        public void onMenuData(String menu) {
            //Display menu data
        }

        @Override
        public void onMrz(String mrz) {
            //Display mrz data
        }
};
```
 * Instantiate it after power up:

```java
MrzReader.Builder.get()
                .setListener(mrzListener)
                .withPort(CpcDefinitions.OCR_READER_PORT_CONE)
                .withBaudrate(CpcDefinitions.OCR_READER_BAUDRATE_CONE)
                .build(context,this);
```

 * Where your activity implements InstanceListener<MrzReader>, get the reader object

```java
@Override
public void onCreated(MrzReader mrzReader) {
    this.mrzReader = mrzReader;    
}

@Override
public void onDisposed(MrzReader mrzReader) {

}
```

### Open reader
```java
mrzReader.open();
```

Reader is fully initialized and is able to receive Mrz data in the listener.

### Known issue

When OCR is powered-on on an Android device, it is recognized as a new keyboard input, and leads to a change of configuration on Activity. This can lead to have Activity being recreated.
To avoid such a behavior, it is recommended to handle the configuration changes in the AndroidManifest

```
https://developer.android.com/guide/topics/resources/runtime-changes#HandlingTheChange
https://developer.android.com/guide/topics/manifest/activity-element
```

For example
```
android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|keyboard|locale"
```