package swen101_04;

import robocode.*;
import robocode.Robot;

import java.awt.*;

public class Emma extends Robot {

    @Override
    public void run() {
        this.setColors(Color.black, Color.blue, Color.yellow);
        while(true) {
            //This will turn the radar right 1 degree every tick.
            this.turnRadarRight(1);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        //We scanned a robot with our radar
        super.onScannedRobot(event);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        //We have been hit
        super.onHitByBullet(event);
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
        //We hit another robot (two tanks collide)
        super.onHitRobot(event);
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        //We hit a wall
        super.onHitWall(event);
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        //We hit another tank with our bullet
        super.onBulletHit(event);
    }
}
