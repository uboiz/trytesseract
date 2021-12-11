package com.jetlab.bacaktp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity
{
    private ActivityResultLauncher activityResultLauncher;
    private ActivityResultLauncher<String> activityOpenImage;
    private ImageView imageView;
    private TessBaseAPI tessBaseAPI;
    public static final String TESS_DATA = "/tessdata";
    private static Uri photoUri;
    private static int SCALEIMAGE=50,BTSBLACKWHITE=100,BAHASA=0;
    private Bitmap tmpbmp=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        imageView=(ImageView)findViewById(R.id.imageview1);

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    preCooking();
                }
            }
        });

        activityOpenImage = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri uri) {
                if (uri!=null) {
                    photoUri=uri;
                    preCooking();
                }
            }
        });

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.lang_option, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(BAHASA);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent, View view,int pos, long id) {
                BAHASA=pos;
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        ((Button)findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                boolean passed=false;
                int n=0;
                while(n<2 && !passed){
                    File file = null;
                    try {
                        if(n==0)file = createImageFile();
                        else file = createImageFile2();
                    } catch (IOException e) {
                        makeToast("Failed make file :"+e.toString());
                    }
                    if (file != null) {
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT){
                            photoUri = Uri.fromFile(file);
                        } else {
                            photoUri = FileProvider.getUriForFile(
                                    getApplicationContext(),
                                    BuildConfig.APPLICATION_ID + "." + getLocalClassName() + ".provider",
                                    file);
                        }

                        intent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
                    }
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        activityResultLauncher.launch(intent);
                        passed=true;
                        break;
                    }
                    n++;
                }

                if(!passed){
                    makeToast("There is no app that support this action");
                }
            }
        });

        ((Button)findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityOpenImage.launch("image/*");
            }
        });

        ((Button)findViewById(R.id.button3)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputNilai(0);
            }
        });

        ((Button)findViewById(R.id.button4)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputNilai(1);
            }
        });

        ((Button)findViewById(R.id.button5)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                letCooking();
            }
        });

        ((Button)findViewById(R.id.button6)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://edugameapp.blogspot.com"));
                startActivity(browserIntent);
            }
        });

        checkPermission();
        prepareTessData();
        updateButton();
    }

    private void updateButton()
    {
        ((Button)findViewById(R.id.button3)).setText("Scale:"+SCALEIMAGE+"%");
        ((Button)findViewById(R.id.button4)).setText("BW:"+BTSBLACKWHITE);
    }

    private void lihatlah(String str)
    {
        ((TextView) findViewById(R.id.textview2)).setText(str);
    }

    private void letCooking()
    {
        if(tmpbmp!=null){
            lihatlah("Scanning..");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String str = getText(tmpbmp);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lihatlah(str);
                        }
                    });
                }
            }).start();
        }else lihatlah("Bitmap is empty!");
    }

    private void preCooking()
    {
        if (photoUri != null){
            lihatlah("Please wait");
            tmpbmp=grabImage(photoUri);
            imageView.setImageBitmap(tmpbmp);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int MAXWIDTHBMP=(int)Math.round(SCALEIMAGE*tmpbmp.getWidth());
                    if(tmpbmp.getWidth()>MAXWIDTHBMP){
                        int w=MAXWIDTHBMP;
                        int h=(int)Math.round(1.0*w*tmpbmp.getHeight()/tmpbmp.getWidth());
                        tmpbmp=Bitmap.createScaledBitmap(tmpbmp,w,h, true);
                    }
                    tmpbmp=toBlackWhite(tmpbmp);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(tmpbmp);
                            lihatlah("Ready to scan");
                        }
                    });
                }
            }).start();
        }else makeToast("Uri is Null");
    }

    private void inputNilai(final int mode)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        if(mode==0)alertDialog.setTitle("Scale");
        else alertDialog.setTitle("Black and White Limit");
        alertDialog.setIcon(R.mipmap.ic_launcher);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.inputnilai,null);
        alertDialog.setView(view);
        final EditText dividen = (EditText)view.findViewById(R.id.edittext1);
        if(mode==0){
            dividen.setText(""+SCALEIMAGE);
            ((TextView)view.findViewById(R.id.textview1)).setText("input 0-100");
        }else{
            dividen.setText(""+BTSBLACKWHITE);
            ((TextView)view.findViewById(R.id.textview1)).setText("input 0-255");
        }
        alertDialog.setCancelable(false);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "SUBMIT",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(dividen!=null && !dividen.getText().toString().isEmpty()){
                            if(mode==0){
                                int a=0;
                                try{
                                    a=Integer.parseInt(dividen.getText().toString());
                                    if(a<0)a=0;
                                    if(a>100)a=100;
                                    SCALEIMAGE=a;
                                    updateButton();
                                    preCooking();
                                }catch (NumberFormatException ex){}
                            }else{
                                int a=0;
                                try{
                                    a=Integer.parseInt(dividen.getText().toString());
                                    if(a<0)a=0;
                                    if(a>255)a=255;
                                    BTSBLACKWHITE=a;
                                    updateButton();
                                    preCooking();
                                }catch (NumberFormatException ex){}
                            }
                        }
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,"CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        if(!isFinishing())alertDialog.show();
    }

    private void makeToast(String txt)
    {
        Toast.makeText(MainActivity.this, txt,Toast.LENGTH_SHORT).show();
    }

    private void checkPermission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 121);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 122);
        }
    }

    private void prepareTessData()
    {
        try{
            File dir = getExternalFilesDir(TESS_DATA);
            if(!dir.exists()){
                if (!dir.mkdir()) {
                    makeToast("Cannot make Dir");
                }
            }
            String fileList[] = getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = dir + "/" + fileName;
                int ino=fileName.lastIndexOf(".");
                String extension = (ino>=0)?fileName.substring(ino):"";
                if(!(new File(pathToDataFile)).exists() && extension.equals(".traineddata")){
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            makeToast("Error :"+e.getMessage());
        }
    }

    public Bitmap toBlackWhite(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for(int i=0;i<bmpOriginal.getWidth();i++){
            for(int j=0;j<bmpOriginal.getHeight();j++){
                int pixel=bmpOriginal.getPixel(i,j);
                int red = Color.red(pixel);
                int blue = Color.blue(pixel);
                int green = Color.green(pixel);

                red=(int)Math.round(1.0*(red+blue+green)/3);
                if(red>255)red=255;

                if(red>BTSBLACKWHITE)red=255;
                else red=0;

                bmpGrayscale.setPixel(i, j, Color.rgb(red, red, red));
            }
        }
        return bmpGrayscale;
    }

    private File createImageFile() throws IOException
    {
        String timeStamp = "edugameapp";
        File storageDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(storageDir,timeStamp+".jpg");
    }

    private File createImageFile2() throws IOException
    {
        String timeStamp = "edugameapp";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir,timeStamp+".jpg");
    }


    private String getText(Bitmap bitmap)
    {
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            makeToast(e.getMessage());
        }
        String dataPath = getExternalFilesDir("/").getPath() + "/";
        if(BAHASA==1)tessBaseAPI.init(dataPath, "ind");
        else tessBaseAPI.init(dataPath, "eng");
        tessBaseAPI.setImage(bitmap);
        String retStr = "No result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            makeToast(e.getMessage());
        }
        tessBaseAPI.end();
        return retStr;
    }

    public Bitmap grabImage(Uri url)
    {
        this.getContentResolver().notifyChange(url, null);
        ContentResolver cr = this.getContentResolver();
        Bitmap bitmap=null;
        try
        {
            bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr,photoUri);
        }
        catch (Exception e)
        {
            makeToast("Failed to load");
        }
        return bitmap;
    }
}