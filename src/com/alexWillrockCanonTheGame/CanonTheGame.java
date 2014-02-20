package com.alexWillrockCanonTheGame;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class CanonTheGame extends Activity {

    private GestureDetector gestureDetector; //прослушка дабл тапов
    private CanonView canonView; //экран игры

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        canonView = (CanonView) findViewById(R.id.cannonView);
        gestureDetector = new GestureDetector(this, gestureListener);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onPause() {
        super.onPause();
        canonView.stopGame(); //завершение игры
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        canonView.releaseResources(); //выгрузка из памяти
    }

    @Override
    public boolean OnTouchEvent(MotionEvent event){ //событие при касании экрана

        int action = event.getAction(); // какое действие произошло

        if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE){

            canonView.allignCannon(event); //нацеливание пушки
        }

        return gestureDetector.onTouchEvent(event);
    }
    GestureDetector.SimpleOnGestureListener // обработка событий по касанию
            gestureListener = new GestureDetector.SimpleOnGestureListener(){

        @Override
        public boolean onDoubleTap(MotionEvent e){
            canonView.fireCannonball(e); //стрельба ядром
            return true; //событие обрабатывается
        }
    };
}
