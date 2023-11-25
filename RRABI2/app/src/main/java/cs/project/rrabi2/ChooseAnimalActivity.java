package cs.project.rrabi2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class ChooseAnimalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_animal);
    }

    public void onCatClicked(View view) {
        Intent intent = new Intent(ChooseAnimalActivity.this, ClassifyCat.class);
        startActivity(intent);
    }

    public void onDogClicked(View view) {
        Intent intent = new Intent(ChooseAnimalActivity.this, ClassifyDog.class);
        startActivity(intent);
    }
}
