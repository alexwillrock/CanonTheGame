package com.alexWillrockCanonTheGame;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.SoundPool;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
   }
