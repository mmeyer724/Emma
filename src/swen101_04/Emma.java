package swen101_04;

import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Emma extends AdvancedRobot {

    private final double INSIDE_BOX_PERCENT = 0.85;
    private Rectangle2D insideBox;

    private Point2D.Double ourPoint;
    private double ourHeading;
    private double ourGunHeading;
    private double ourRadarHeading;

    private Point2D.Double enemyPoint;
    private double oldEnemyHeading;
    private String enemyName = "";
    private int amountHit = 0;
    private int missedBullets = 0;

    private ArrayList<Point2D.Double> sections;
    private EmmaThread bestSectionThread;
    private final EmmaEscape escapePoint = new EmmaEscape(null, false);
    private final ConcurrentHashMap<String, Point2D.Double> aroundMe = new ConcurrentHashMap<String, Point2D.Double>();
    private final ConcurrentHashMap<String, Rectangle2D.Double> aroundMeCollisions = new ConcurrentHashMap<String, Rectangle2D.Double>();


    @Override
    public void run() {
        this.setColors(Color.black, Color.blue, Color.yellow);

        this.sections = new ArrayList<Point2D.Double>();

        Rectangle2D battlefield = new Rectangle2D.Double(0, 0, this.getBattleFieldWidth(), this.getBattleFieldHeight());
        this.insideBox = getSmallerBox(battlefield, INSIDE_BOX_PERCENT);

        double sectWidth = this.insideBox.getWidth() / 6;
        final double sectHeight = this.insideBox.getHeight() / 6;
        double boxX = this.insideBox.getX();
        double boxY = this.insideBox.getY();

        for(double x = (sectWidth/2); x <= this.insideBox.getWidth(); x += sectWidth) {
            for(double y = (sectHeight/2); y <= this.insideBox.getHeight(); y += sectHeight) {
                this.sections.add(new Point2D.Double(x + boxX, y + boxY));
            }
        }

        //Make everything turn independently
        this.setAdjustGunForRobotTurn(true);
        this.setAdjustRadarForGunTurn(true);
        this.setAdjustRadarForRobotTurn(true);

        this.bestSectionThread = new EmmaThread();
        this.bestSectionThread.start();

        boolean justStarted = true;

        while (true) {
            if(justStarted) {
                justStarted = false;
                this.turnRadarRight(360);
                this.escape();
            }

            if(!this.isLockedOn()) {
                this.lockOnToNearest();
            }

            this.setTurnRadarRight(360);

            this.execute();
        }
    }

    private boolean shouldMove() {
        if(this.getEnergy() < 50) {
            if(this.amountHit >= 1) {
                return true;
            } else {
                return false;
            }
        } else {
            if(this.amountHit >= 3) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        double bearing = event.getBearing();
        double distance = event.getDistance();
        Point2D.Double botPoint = this.getPointByBearingDistance(bearing, distance);
        this.aroundMe.put(event.getName(), botPoint);

        if(this.isTrackedEnemy(event.getName())) {
            //Variables for circular targeting
            double bulletPower = Math.min(6.0, getEnergy());
            double myX = getX();
            double myY = getY();
            double absoluteBearing = this.getHeadingRadians() + event.getBearingRadians();
            double enemyX = getX() + event.getDistance() * Math.sin(absoluteBearing);
            double enemyY = getY() + event.getDistance() * Math.cos(absoluteBearing);
            double enemyHeading = event.getHeadingRadians();
            double enemyHeadingChange = enemyHeading - this.oldEnemyHeading;
            double enemyVelocity = event.getVelocity();
            this.oldEnemyHeading = enemyHeading;
            double deltaTime = 0;
            double battleFieldHeight = getBattleFieldHeight();
            double battleFieldWidth = getBattleFieldWidth();
            double predictedX = enemyX, predictedY = enemyY;

            //Predict where the enemy will be next
            while ((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, predictedX, predictedY)) {
                predictedX += Math.sin(enemyHeading) * enemyVelocity;
                predictedY += Math.cos(enemyHeading) * enemyVelocity;
                enemyHeading += enemyHeadingChange;
                if (predictedX < 18.0
                        || predictedY < 18.0
                        || predictedX > battleFieldWidth - 18.0
                        || predictedY > battleFieldHeight - 18.0) {

                    predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);
                    predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
                    break;
                }
            }
            double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));

            //Turn radar and gun to the predicted location
            setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
            setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));

            this.setFire(bulletPower);
            this.setFire(bulletPower);

            if(!this.shouldMove()) {
                this.facePoint(botPoint, EmmaPart.RADAR);
                this.scan();
            } else {
                this.turnRadarRight(360);
                this.escape();
            }
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent event) {
        String name = event.getName();
        this.aroundMe.remove(name);
        this.aroundMeCollisions.remove(name);
        if (this.isTrackedEnemy(name)) {
            this.lockOffEnemy();
        }
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
        this.turnRadarRight(360);
        this.escape();
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        this.escape();
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        this.missedBullets++;
        if(this.missedBullets > 2) {
            this.missedBullets = 0;
            this.lockOffEnemy();
            this.lockOnToNearest();
        }
    }

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        this.amountHit++;
        if(this.shouldMove()) {
            this.escape();
        }
        this.lockOnToNearest();
    }

    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        this.escapePoint.setEndThread(true);
    }

    @Override
    public void onWin(WinEvent event) {
        this.escapePoint.setEndThread(true);
    }

    @Override
    public void onDeath(DeathEvent event) {
        this.escapePoint.setEndThread(true);
    }

    @Override
    public void onPaint(Graphics2D g) {
        g.setColor(Color.green);
        g.drawRect((int) insideBox.getX(), (int) insideBox.getY(), (int) insideBox.getWidth(), (int) insideBox.getHeight());

        g.setColor(Color.BLUE);
        for(Point2D.Double point : this.sections) {
            g.drawOval((int)point.getX(), (int)point.getY(), 10, 10);
        }

        Point2D.Double ePoint = this.escapePoint.getPoint();
        if(ePoint != null) {
            g.setColor(Color.RED);
            g.fillOval((int)ePoint.getX(), (int)ePoint.getY(), 10, 10);
            Line2D.Double eRoute = new Line2D.Double(this.ourPoint, ePoint);
            g.setColor(Color.GREEN);
            g.drawLine((int)eRoute.getX1(), (int)eRoute.getY1(), (int)eRoute.getX2(), (int)eRoute.getY2());
        }

        for(Rectangle2D.Double eRect : this.aroundMeCollisions.values()) {
            g.setColor(Color.PINK);
            g.drawRect((int) eRect.getX(), (int) eRect.getY(), (int) eRect.getWidth(), (int) eRect.getHeight());
        }
    }

    @Override
    public void onStatus(StatusEvent e) {
        RobotStatus status = e.getStatus();
        this.ourPoint = new Point2D.Double(status.getX(), status.getY());
        this.ourHeading = status.getHeading();
        this.ourGunHeading = status.getGunHeading();
        this.ourRadarHeading = status.getRadarHeading();
    }

    private void lockOnToNearest() {
        System.out.println("Attempting lock on...");
        String target = "";
        double closestDist = -1;
        for(Map.Entry<String, Point2D.Double> bot : this.aroundMe.entrySet()) {
            double dist = bot.getValue().distance(this.ourPoint);
            if(closestDist < 0 || dist <= closestDist) {
                target = bot.getKey();
                closestDist = dist;
            }
        }
        Point2D.Double enemyPoint = this.aroundMe.get(target);
        if(enemyPoint != null) {
            this.enemyName = target;
            this.oldEnemyHeading = 0;
            this.facePoint(this.aroundMe.get(target), EmmaPart.RADAR);
        }
    }

    private void lockOffEnemy() {
        System.out.println("Locking off "+this.enemyName);
        this.enemyName = "";
    }

    private boolean isTrackedEnemy(String name) {
        return this.enemyName == name;
    }

    private boolean isLockedOn() {
        return !this.enemyName.isEmpty();
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

    private void escape() {
        Point2D.Double ePoint = this.escapePoint.getPoint();
        if(ePoint != null) {
            System.out.println("Escaping!");
            this.facePoint(ePoint, EmmaPart.BODY);
            this.setAhead(ePoint.distance(this.ourPoint));
        }
        this.amountHit = 0;
        this.execute();
    }

    private Point2D.Double getPointByBearingDistance(double bearing, double distance) {
        double absBearing = Math.toRadians(bearing + this.ourHeading);
        double enx = Math.sin(absBearing) * distance;
        double eny = Math.cos(absBearing) * distance;
        double enemyX = this.ourPoint.getX() + enx;
        double enemyY = this.ourPoint.getY() + eny;
        return new Point2D.Double(enemyX, enemyY);
    }

    private void facePoint(Point2D.Double point, EmmaPart part) {
        double angle = this.getAbsoluteBearingTo(point);
        switch (part) {
            case BODY:
                angle -= this.ourHeading;
                this.setTurnRight(Utils.normalRelativeAngleDegrees(angle));
                break;
            case GUN:
                angle -= this.ourGunHeading;
                this.setTurnGunRight(Utils.normalRelativeAngleDegrees(angle));
                break;
            case RADAR:
                angle -= this.ourRadarHeading;
                this.setTurnRadarRight(Utils.normalRelativeAngleDegrees(angle));
                break;
        }
    }

    private double getAbsoluteBearingTo(Point2D.Double point) {
        double xDiff = point.getX() - this.ourPoint.getX();
        double yDiff = point.getY() - this.ourPoint.getY();
        return Utils.normalAbsoluteAngleDegrees((Math.toDegrees(Math.atan2(yDiff, xDiff)) * -1) + 90);
    }

    enum EmmaPart {
        BODY,
        GUN,
        RADAR;
    }

    class EmmaEscape {
        private Point2D.Double point;
        private boolean endThread;

        EmmaEscape(Point2D.Double point, boolean endThread) {
            this.point = point;
            this.endThread = endThread;
        }

        public Point2D.Double getPoint() {
            return point;
        }

        public void setPoint(Point2D.Double point) {
            this.point = point;
        }

        public boolean isEndThread() {
            return endThread;
        }

        public void setEndThread(boolean endThread) {
            this.endThread = endThread;
        }

        public Point2D.Double getOurPoint() {
            return ourPoint;
        }
    }

    class EmmaThread extends Thread implements Runnable {
        @Override
        public void run() {
            while(!escapePoint.isEndThread()) {
                double farthestDist = 0;
                for (Point2D.Double sectPoint : sections) {
                    Line2D.Double line = new Line2D.Double(escapePoint.getOurPoint(), sectPoint);
                    double closestDist = -1;
                    for (Map.Entry<String, Point2D.Double> bot : aroundMe.entrySet()) {
                        Point2D.Double loc = bot.getValue();
                        Rectangle2D.Double collision = new Rectangle2D.Double(loc.getX() - 50, loc.getY() - 50, 100, 100);
                        aroundMeCollisions.put(bot.getKey(), collision);
                        if(line.intersects(collision)) {
                            break;
                        }
                        double dist =loc.distance(sectPoint);
                        if (closestDist == -1 || dist < closestDist) {
                            closestDist = dist;
                        }
                    }
                    if (farthestDist < closestDist) {
                        farthestDist = closestDist;
                        escapePoint.setPoint(sectPoint);
                    }
                }
            }
        }
    }
}