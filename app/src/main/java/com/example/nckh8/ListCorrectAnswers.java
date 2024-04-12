package com.example.nckh8;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListCorrectAnswers extends AppCompatActivity {

    String[] numericalOrder;
    ListView lvCorrectAnswers;
    Button btnSelectImage, btnBackMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_correct_answers);

        lvCorrectAnswers = findViewById(R.id.lv_correct_answers);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnBackMain = findViewById(R.id.btn_back_main);

        // Tải thư viện OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("TAG", "OpenCV library not loaded");
        } else {
            Log.d("TAG", "OpenCV library loaded successfully");
        }

        Intent intent = getIntent();
        String number = intent.getStringExtra("number");

        if (number != null) {
            numericalOrder = new String[Integer.parseInt(number)];

            for (int i = 0; i < numericalOrder.length; i++) {
                if (i<9) {
                    numericalOrder[i] = "0" + (i+1);
                    continue;
                }
                numericalOrder[i] = String.valueOf((i+1));
            }

            // Đặt bộ điều hợp để điền dữ liệu vào ListView
            AdapterCorrectAnswer adapterCorrectAnswer = new AdapterCorrectAnswer(getApplicationContext(), numericalOrder);
            lvCorrectAnswers.setAdapter(adapterCorrectAnswer);

            btnSelectImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent,1000);
                }
            });

            btnBackMain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000 && data != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());

                Mat img = new Mat();
                Utils.bitmapToMat(bitmap, img);

                List<Mat> cropImages = OMR2.cropImage(img);
                List<Mat> listAnswers = OMR2.processAnsBlocks(cropImages);
                List<Mat> processListAnswers = OMR2.processListAns(listAnswers);
                ArrayList<String> studentAnswers = new ArrayList<>();

                float totalScore;
                int numberCorrectAnswers = 0;
                int multipleAnswer = 0;
                boolean correctAnswer = false;


                for (int i = 0; i < numericalOrder.length*4; i++) {
                    Mat ans = processListAnswers.get(i);
                    int tes = Core.countNonZero(ans);
                    String mappedAnswer = OMR2.mapAnswer(i);

                    // Duyệt hết 1 câu
                    if (i % 4 == 0) {
                        studentAnswers.add("Null");
                        multipleAnswer = 0;
                        correctAnswer = false;
                    }
                    Log.d("check",""+i +" omr : "+tes);

                    if (tes > 170) {
                        if (multipleAnswer > 0 ) {
                            studentAnswers.set(i/4,"Null");
                            if (correctAnswer) {
                                numberCorrectAnswers--;
                                correctAnswer = false;
                            }
                        } else {
                            studentAnswers.set(i/4,mappedAnswer);
                            if (AdapterCorrectAnswer.correctAnswers.get(i / 4).equals(mappedAnswer)) {
                                numberCorrectAnswers++;
                                correctAnswer = true;
                            }
                        }
                        multipleAnswer++;
                    }

//                    Log.d("CHECK", i + " " + String.valueOf(correctAnswer));
//                    Log.d("CHECK", i + " " + numberCorrectAnswers);
                }


                totalScore = (float) numberCorrectAnswers / numericalOrder.length * 10;

                Intent intent = new Intent(ListCorrectAnswers.this, ListStudentAnswers.class);

                // Lưu Bitmap vào bộ nhớ
                String imagePath = saveBitmapToStorage(bitmap);

                // Gửi đường dẫn hình ảnh thay vì Bitmap qua Intent
                intent.putExtra("image_path", imagePath);
                intent.putExtra("numerical_order", numericalOrder);
                intent.putExtra("total_score", totalScore);
                intent.putExtra("student_answers", studentAnswers);
                intent.putExtra("correct_answers", AdapterCorrectAnswer.correctAnswers);

                startActivity(intent);

            } catch (Exception e) {
                e.printStackTrace();
                // Xử lý ngoại lệ khác nếu cần
                Toast.makeText(ListCorrectAnswers.this, "Phiếu không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private String saveBitmapToStorage(Bitmap bitmap) {
        String path = ""; // Đường dẫn lưu trữ ảnh

        try {
            // Tạo thư mục lưu trữ (nếu chưa tồn tại)
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = File.createTempFile("image", ".jpg", storageDir);

            // Ghi Bitmap vào tệp
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            // Giải phóng bộ nhớ của Bitmap
            bitmap.recycle();

            path = imageFile.getAbsolutePath(); // Lấy đường dẫn tệp
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(ListCorrectAnswers.this, "Phiếu không hợp lệ", Toast.LENGTH_SHORT).show();

        }

        return path;
    }
}