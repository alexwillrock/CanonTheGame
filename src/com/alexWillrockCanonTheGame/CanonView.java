package com.alexWillrockCanonTheGame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.HashMap;
import java.util.Map;

public class CanonView extends SurfaceView implements SurfaceHolder.Callback{

    private CannonThread cannonThread; // класс контроль цикла игры
    private Activity activity;// рисуем диалог завершения игры

    private boolean dialogIsDisplayed = false;

    public static final int TARGET_PIECES = 7; //секции цели
    public static final int MISS_PENALTY = 2; //в случае промаха убавляем 2 сек
    public static final int HIT_REWARD = 3; //при попадании добавляем 3 сек

    //цикл и статистика
    private boolean gameOver; //завершена ли игра
    private double timeLeft; //время осталось
    private int shotfFired; //колличество выстрелов
    private double totalTimeElapsed; //времени прошло

    //определяю блока
    private Line blocker; //начало и конец линии
    private int blockerDistance; //от блока слева
    private int blockedBeginning; //от блока сверхку
    private int blockerEnd; //от блока снизу
    private int initialBlockerVelocity; //ускроение блока
    private float blockerVelocity; //ускорение во время игры

    //определение цели
    private Line target;//накало и конец мишени
    private int targetDistance; //от мишени слева
    private int targetBeginning; //от мишени сверхку
    private double pieceLength; //длина секции мишени
    private int targetEnd; //от мишени снизу

    private int initialTargetVelocity; //ускорение мишени
    private float targetVelocity; //ускорение во время игры

    //общие парамы
    private int lineWidth; //ширига блока и мишени
    private boolean[] hitStates; //все секции уничтожены
    private int targetPiecesHit; //сколько мишеней уничтожено

    // ядро
    private Point cannonball; //картинка ядра
    private int cannonballVelocityX; // ускорение по Х
    private int cannonballVelocityY; // ускорение по Y

    private boolean cannonballOnScreen; //ядро на экране
    private int cannonballRadius; //радиус ядра
    private int cannonballSpeed; //скорость ядра

    //пушка
    private int cannonBaseRadius; //радиус основания пушки
    private int cannonLength; //длина пушки
    private Point barrelEnd; //конец пушки
    private int screenWidth; //ширина экрана
    private int screenHeigth; //высота экрана

    //звук
    private static final int TARGET_SOUND_ID = 0;
    private static final int CANNON_SOUND_ID = 1;
    private static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool; // эффект звука
    private Map<Integer, Integer> soundMap; //id для эффекта

    //рисую на экране
    private Paint textPaint; //текст
    private Paint cannonballPaint; //ядро
    private Paint cannonPaint; //пушка
    private Paint blockerPaint; //блок
    private Paint targetPaint; //мишень
    private Paint backgroundPaint; //для обнуления

    public CanonView(Context context, AttributeSet attrs){
        super(context, attrs);
        activity = (Activity) context;

        getHolder().addCallback(this); //слушатель нажитий
        blocker = new Line();
        target = new Line();
        cannonball = new Point();

        hitStates = new boolean[TARGET_PIECES];

        //звук c одним потоком
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        soundMap = new HashMap<Integer, Integer>();
        soundMap.put(TARGET_SOUND_ID, soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID, soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID, soundPool.load(context, R.raw.blocker_hit, 1));

        textPaint = new Paint();
        cannonballPaint = new Paint();
        blockerPaint = new Paint();
        targetPaint = new Paint();
        backgroundPaint = new Paint();
    }

    @Override
    protected  void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w;
        screenHeigth = h;


        //пушка
        cannonBaseRadius = h / 18; //радиус основания в маштабе
        cannonLength = w / 8; //длина в маштабе
        barrelEnd = new Point(cannonLength, h / 2); //конец пушки

                //ядро
        cannonballRadius = w / 36; //ядро в маштабе
        cannonballSpeed = w * 3 / 2; //скорость в маштабе


        //блок
        lineWidth = w / 24; //ширина блока и мишени в маштабе

        blockerDistance = w * 5 / 8; // ширина поля
        blockedBeginning = h / 8; // от верха до блока
        blockerEnd = h * 3 / 8; //снизу до блока
        initialBlockerVelocity = h / 2; //начальная скорость

        //задаем начальное положение блока
        blocker.start = new Point(blockerDistance, blockedBeginning);
        blocker.end = new Point(blockerDistance, blockerEnd);

        //мишень
        targetDistance = w * 7 / 8;
        targetBeginning = h / 8;
        targetEnd = h * 7 / 8;
        pieceLength = (targetEnd - targetBeginning) / TARGET_PIECES;
        initialTargetVelocity = -h / 4;
        target.start = new Point(targetDistance, targetBeginning);
        target.end = new Point(targetDistance, targetEnd);

        //элементы игры
        textPaint.setTextSize(w / 20);
        textPaint.setAntiAlias(true); //сглаживание
        cannonPaint.setStrokeWidth(lineWidth * 1.5f); //толщани линии
        targetPaint.setStrokeWidth(lineWidth);
        blockerPaint.setStrokeWidth(lineWidth);

        backgroundPaint.setColor(Color.WHITE); //цвет фона

        newGame();// запуск игры
    }

    public void newGame(){

        for(int i = 0; i < TARGET_PIECES; ++i){ //восстанавливаем все мишени
            hitStates[i] = false;
        }

        targetPiecesHit = 0; //обнулить попдания
        blockerVelocity = initialBlockerVelocity; //скорость по началу блока
        targetVelocity = initialTargetVelocity; //скорость по начала мишени

        timeLeft = 10; //обратный отсчет
        cannonballOnScreen = false; //ядра нет на экране
        shotfFired = 0; //обнулились выстрелы

        totalTimeElapsed = 0.0; // начальное время

        blocker.start.set(blockerDistance, blockedBeginning);
        blocker.end.set(targetDistance, blockerEnd);

        target.start.set(targetDistance, targetBeginning);
        target.end.set(blockerDistance, targetEnd);

        if(gameOver){
            gameOver = false; //игра не завершена
            cannonThread = new CannonThread(getHolder());
            cannonThread.start();
        }
    }

    //обновление элементов игры
    private void updatePositions(double elapsedTimeMS){
        double interval = elapsedTimeMS / 1000.0;
        if(cannonballOnScreen){ //если выстрел ядро на экране
            cannonball.x += interval * cannonballVelocityX;
            cannonball.y += interval * cannonballVelocityY;

            //столкновение с блоков
            if(cannonball.x + cannonballRadius > blockerDistance
                    && cannonball.x - cannonballRadius < blockerDistance
                    && cannonball.y + cannonballRadius > blocker.start.y &&
                    cannonball.y - cannonballRadius < blocker.end.y){

                cannonballVelocityX *= - 1; //ядро ударяется и летит обратно
                timeLeft -= MISS_PENALTY; //штраф игрока

                soundPool.play(soundMap.get(BLOCKER_SOUND_ID), 1, 1, 1, 0, 1f);
            }
            else
                if(cannonball.x + cannonballRadius > screenHeigth ||
                        cannonball.x - cannonballRadius < 0){
                    cannonballOnScreen = false; //улетел
                }
                else
                    if(cannonball.y + cannonballRadius > screenHeigth
                            ||cannonball.y - cannonballRadius < 0){
                        cannonballOnScreen = false; //улетел
                    }
                    else //столкуля с мишенью
                        if(cannonball.x + cannonballRadius > targetDistance
                                && cannonball.x - cannonballRadius < targetDistance
                                && cannonball.y + cannonballRadius > target.start.y
                                && cannonball.y - cannonballRadius < target.end.y){
                                    //определяем номер секции
                                    int section = (int) ((cannonball.y - target.start.y) / pieceLength);

                                    if ((section >= 0 && section < TARGET_PIECES) && !hitStates[section]){
                                        hitStates[section] = true; //попал
                                        cannonballOnScreen = false; //ядро улетело
                                        timeLeft += HIT_REWARD; //поблажка

                                        soundPool.play(soundMap.get(TARGET_SOUND_ID), 1, 1, 1,0, 1f);

                                        if(++targetPiecesHit == TARGET_PIECES){
                                            cannonThread.setRunning(false);
                                            showGameDialog(R.string.win);
                                            gameOver = true; //игра завершена
                                        }
                                    }
                        }
        }

        //обновление позиции блока
        double blockerUpdate = interval * blockerVelocity;
        blocker.start.y += blockerUpdate;
        blocker.end.y += blockerUpdate;

        //обновлеение позиции мишени
        double targetUpdate = interval * targetVelocity;
        target.start.y += targetUpdate;
        target.end.y += targetUpdate;

        //обратное движение при достижении конца экрана
        if(blocker.start.y < 0 || blocker.end.y > screenHeigth){
            blockerVelocity *= -1;
        }
        if(target.start.y < 0 || target.end.y > screenHeigth){
            targetVelocity *= -1;
        }

        timeLeft -= interval; //изменяем время
        if(timeLeft <= 0){
            timeLeft = 0.0;
            gameOver = true; //игра закончилась
            cannonThread.setRunning(false);
            showGameOvweDialog(R.string.lose);
        }
    }

    //выстрел
    public void fireCannonball(MotionEvent event){
        if(cannonballOnScreen){ ///если ядро на экране
             return; //выключить
        }

        double angle = alingCannon(event); //угол наклона пушки

        cannonball.x = cannonballRadius; //налон по х
        cannonball.y = screenHeigth / 2; //выстрел по вертикали

        cannonballVelocityX = (int) (cannonballSpeed * Math.sin(angle)); // x - компонента скорости
        cannonballVelocityY = (int) (cannonballSpeed * Math.cos(angle)); // y - комнонета скорости

        cannonballOnScreen = true; //отображение на экране
        ++ shotfFired;

        soundPool.play(soundMap.get(CANNON_SOUND_ID), 1, 1, 1, 0, 1f);
    }

    //наклон пушки
    public double alingCannon(MotionEvent event){

        Point touchPoint = new Point((int) event.getX(), (int) event.getY()); // место касания
        double centerMinusY = (screenHeigth / 2 - touchPoint.y); //расстояние до центра экрана

        double angle = 0; //угол

        if(centerMinusY != 0){ //если коснулиь верх
            angle = Math.atan((double) touchPoint.x / centerMinusY); // угол наклона относительно горизонта
        }

        if(touchPoint.y > screenHeigth / 2){ //если коснулись низ
            angle += Math.PI;
        }

        //конечное положение ствола
        barrelEnd.x = (int) (cannonLength * Math.sin(angle));
        barrelEnd.y = (int) ( -cannonLength * Math.cos(angle) + screenHeigth / 2);

        return angle;
    }

    public void drawGameElements(Canvas canvas){

        //очитска экрана
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        //таймер оставшегося времени
        canvas.drawText(getResources().getString(R.string.time_remaing_format, timeLeft), 30, 50, textPaint);

        if(cannonballOnScreen){ //рисуем ядро на экран
            canvas.drawCircle(cannonball.x, cannonball.y, cannonballRadius, cannonballPaint);
        }

        canvas.drawLine(0, screenHeigth / 2, barrelEnd.x, barrelEnd.y, cannonPaint); //рисуем ствола пушки

        canvas.drawCircle(0, (int) screenHeigth / 2, (int) cannonBaseRadius, cannonPaint); // рисуем базу пушки

        canvas.drawLine(blocker.start.x, blocker.start.y, blocker.end.x, blocker.end.y, blockerPaint); //рисуем блок

        Point currentPoint = new Point(); //начало текущей секции мишени

        currentPoint.x = target.start.x;
        currentPoint.y = target.start.y;

        //рисуем мишени
        for(int i = 1; i <= TARGET_PIECES; ++i){

            if(!hitStates[i - 1]){ //если не попал то рисуем
                if(i % 2 == 0){ //чередовашка
                    targetPaint.setColor(Color.YELLOW);
                }
                else{
                    targetPaint.setColor(Color.BLUE);
                }
                canvas.drawLine(currentPoint.x, currentPoint.y, target.end.x, (int) (currentPoint.y + pieceLength), targetPaint);
            }
            currentPoint.y += pieceLength; // смещаемся к следующей секции
        }
    }

    private void showGameDialog(int messageId){
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());

        dialogBuilder.setTitle(getResources().getString(messageId)); //создаем диалог сообщения универсальый
        dialogBuilder.setCancelable(false);

        //отображает выстрелы и время общее
        dialogBuilder.setMessage(getResources().getString(
                R.string.result_format, shotfFired, totalTimeElapsed));
        dialogBuilder.setPositiveButton(R.string.reset_game, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogIsDisplayed = false;
                newGame(); //новая игра
            }
        });

        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        dialogIsDisplayed = true;
                        dialogBuilder.show(); //начинаем диалог
                    }
                }
        );

    }

    public void stopGame(){
        if(cannonThread != null){
            cannonThread.setRunning(false);
        }
    }

    public void releaseResouces(){
        soundPool.release();

        soundPool = null;
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int heigth) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    
}
