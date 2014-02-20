package com.alexWillrockCanonTheGame;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
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

}
