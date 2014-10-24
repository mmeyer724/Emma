package swen101_04;

import robocode.*;
import robocode.Robot;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

public class Emma extends Robot {

    //The size of the inside box (percent of battlefield)
    private static double INSIDE_BOX_PERCENT = 0.85;
    //The size of the box inside the inner box (percent of inner box)
    private static double INSIDE_INNER_BOX_PERCENT = 0.50;
    //Rectangle representing the inner box
    private Rectangle2D insideBox;
    //Rectangle representing a box inside the inner box
    private Rectangle2D insideInnerBox;

    @Override
    public void run() {
        this.setColors(Color.black, Color.blue, Color.yellow);

        Rectangle2D battlefield = new Rectangle2D.Double(0, 0, this.getBattleFieldWidth(), this.getBattleFieldHeight());
        insideBox = getSmallerBox(battlefield, INSIDE_BOX_PERCENT);
        insideInnerBox = getSmallerBox(insideBox, INSIDE_INNER_BOX_PERCENT);

        while (true) {
            final double X = this.getX();
            final double Y = this.getY();

            //Detect if we are going past the inside box
            if (!insideBox.contains(X, Y)) {
                this.goTo(this.getRandomPointInRectangle(this.insideInnerBox));
            }

            this.turnRadarRight(10);
            this.ahead(5);
            this.turnRight(2);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        //Lock on to the robot
        final double bearing = event.getBearing();
        final double distance = event.getDistance();
        final Point2D.Double enemyPoint = this.getPointByAngleDistance(bearing, distance);

        //Only attack if the robot is inside our inside box
        if (!this.insideBox.contains(enemyPoint)) {
            System.out.println("Ignoring  " + event.getName() + ", too close to border (or in corner)");
            return;
        }

        this.turnRadarRight(getHeading() - getRadarHeading() + bearing);
        this.turnRight(bearing);

        double enterAtAngle = 25;
        double hypAngle = 180 - (enterAtAngle * 2);
        double side = (Math.sin(Math.toRadians(enterAtAngle)) * distance) / Math.sin(Math.toRadians(hypAngle));

        this.turnRight(enterAtAngle);
        this.ahead(side);
        this.turnLeft(enterAtAngle * 2);
        this.ahead(side);
        this.turnRight(enterAtAngle);

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

    @Override
    public void onPaint(Graphics2D g) {
        //This method allows us to debug by drawing on top of the battlefield
        g.setColor(Color.red);
        g.drawRect((int) insideBox.getX(), (int) insideBox.getY(), (int) insideBox.getWidth(), (int) insideBox.getHeight());
        g.setColor(Color.orange);
        g.drawRect((int) insideInnerBox.getX(), (int) insideInnerBox.getY(), (int) insideInnerBox.getWidth(), (int) insideInnerBox.getHeight());
    }

    private Point2D.Double getPointByAngleDistance(double angle, double distance) {
        double pointX = this.getX() + distance * Math.cos(angle);
        double pointY = this.getY() + distance * Math.sin(angle);
        return new Point2D.Double(pointX, pointY);
    }

    private Point2D.Double getRandomPointInRectangle(Rectangle2D rect) {
        Random rand = new Random();
        double x = rect.getX() + rand.nextInt((int) rect.getWidth());
        double y = rect.getY() + rand.nextInt((int) rect.getHeight());
        return new Point2D.Double(x, y);
    }

    private Rectangle2D getSmallerBox(Rectangle2D big, double percent) {
        double insideWidth = big.getWidth() * percent;
        double insideHeight = big.getHeight() * percent;
        double diffWidth = big.getWidth() - insideWidth;
        double diffHeight = big.getHeight() - insideHeight;
        double startX = (diffWidth / 2) + big.getX();
        double startY = (diffHeight / 2) + big.getY();
        return new Rectangle2D.Double(startX, startY, insideWidth, insideHeight);
    }

    private void facePoint(Point2D point) {
        Point2D.Double location = new Point2D.Double(this.getX(), this.getY());
        double angle = normalRelativeAngle(absoluteBearing(location, point) - getHeading());
        this.turnRight(angle);
    }

    private void goTo(Point2D point) {
        Point2D.Double location = new Point2D.Double(this.getX(), this.getY());
        double distance = location.distance(point);
        double angle = normalRelativeAngle(absoluteBearing(location, point) - getHeading());
        // we can make the robot go backwards instead of rotating all the way in certain cases
        if (Math.abs(angle) > 90.0) {
            distance *= -1.0;
            if (angle > 0.0) {
                angle -= 180.0;
            } else {
                angle += 180.0;
            }
        }
        this.turnRight(angle);
        this.ahead(distance);
    }

    private double absoluteBearing(Point2D source, Point2D target) {
        return Math.toDegrees(Math.atan2(target.getX() - source.getX(), target.getY() - source.getY()));
    }

    private double normalRelativeAngle(double angle) {
        double relativeAngle = angle % 360;
        if (relativeAngle <= -180) {
            return 180 + (relativeAngle % 180);
        } else if (relativeAngle > 180) {
            return -180 + (relativeAngle % 180);
        } else {
            return relativeAngle;
        }
    }
}
