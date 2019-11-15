package com.lukehere.app.accord;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.zxing.Result;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QRScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private static final int REQUEST_CAMERA = 17;
    private ZXingScannerView scannerView;
    private boolean mFlashMode = false;

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(QRScannerActivity.this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(QRScannerActivity.this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        if (scannerView == null) {
            scannerView = new ZXingScannerView(this);
            setContentView(scannerView);
        }

        scannerView.setResultHandler(this);
        scannerView.setAutoFocus(true);

        if (mFlashMode) {
            scannerView.setFlash(true);
        }

        scannerView.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFlashMode) {
            scannerView.setFlash(false);
            scannerView.stopCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scannerView.setFlash(false);
        scannerView.stopCamera();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_scanner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.flash:
                if (!mFlashMode) {
                    item.setIcon(getDrawable(R.drawable.ic_action_flash_on));
                    scannerView.setFlash(true);
                    mFlashMode = true;
                } else {
                    item.setIcon(getDrawable(R.drawable.ic_action_flash_off));
                    scannerView.setFlash(false);
                    mFlashMode = false;
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void handleResult(@NonNull Result result) {
        String scanResult = result.getText();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("Result", scanResult);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Nothing needs to be done. Proceed as usual.
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }
}
