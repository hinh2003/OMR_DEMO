package com.example.nckh8;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

public class ListStudentAnswers extends AppCompatActivity {

    ListView lvStudentAnswers;
    Button btnSelectAnotherImage, btnBackCorrect;
    TextView tvTotalScore;
    ImageView imageView;

    // Biến nhận Intent
    ArrayList<String> studentAnswers, correctAnswers;
    String[] numericalOrder;
    String imagePath;
    float totalScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_student_answers);

        tvTotalScore = findViewById(R.id.tv_total_score);
        lvStudentAnswers = findViewById(R.id.lv_student_answers);
        btnSelectAnotherImage = findViewById(R.id.btn_select_another_image);
        btnBackCorrect = findViewById(R.id.btn_back_correct);
        imageView = findViewById(R.id.imageView);

        Intent intent = getIntent();

        // Nhận đường dẫn ảnh từ Intent
        imagePath = intent.getStringExtra("image_path");
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        studentAnswers = intent.getStringArrayListExtra("student_answers");
        correctAnswers = intent.getStringArrayListExtra("correct_answers");
        totalScore = intent.getFloatExtra("total_score", totalScore);
        numericalOrder = intent.getStringArrayExtra("numerical_order");

        imageView.setImageBitmap(bitmap);
        tvTotalScore.setText("Phiếu được " + String.format("%.2f", totalScore) + " điểm");

        AdapterStudentAnswer adapterStudentAnswer = new AdapterStudentAnswer(getApplicationContext(), numericalOrder, correctAnswers, studentAnswers);
        lvStudentAnswers.setAdapter(adapterStudentAnswer);

        btnSelectAnotherImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,1001);
            }
        });

        btnBackCorrect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && data != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());

                // Giải phóng bộ nhớ của Bitmap hiện tại (nếu có)
                if (imageView.getDrawable() != null) {
                    Bitmap oldBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                    oldBitmap.recycle();
                }

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

                    if (tes > 180) {
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


                // Tính toán điểm số
                totalScore = (float) numberCorrectAnswers / numericalOrder.length * 10;

                // Hiển thị hình ảnh và điểm số tổng cộng trên giao diện người dùng
                imageView.setImageBitmap(bitmap);
                tvTotalScore.setText("Phiếu được " + String.format("%.2f", totalScore) + " điểm");

                // Hiển thị danh sách câu trả lời của học sinh
                AdapterStudentAnswer adapterStudentAnswer = new AdapterStudentAnswer(getApplicationContext(), numericalOrder, correctAnswers, studentAnswers);
                lvStudentAnswers.setAdapter(adapterStudentAnswer);

            } catch (Exception e) {
                e.printStackTrace();

                Toast.makeText(ListStudentAnswers.this, "Phiếu không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        }
    }
}