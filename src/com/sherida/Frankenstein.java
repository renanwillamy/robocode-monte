package com.sherida;

import robocode.*;
import robocode.util.Utils;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Color;

public class Frankenstein extends AdvancedRobot {

	private EnemyBot target = new EnemyBot();
	private Map enemies;

	private Point2D.Double myPos;
	private Point2D.Double lastPosition;
	private Point2D.Double nextDestination;

	public void run() {
		enemies = new HashMap();
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		target.reset();

		setColors(Color.black, Color.black, Color.black);
		nextDestination = lastPosition = myPos = new Point2D.Double(getX(), getY());

		while(true) {
			setTurnRadarRight(360);
			shootLogic();
			movimentationLogic();
			execute();
		}
	}

	private void movimentationLogic() {
		if ("".equals(target.name)) {
			setTurnRight(5);
			setAhead(20);
			return;
		}

		myPos = new Point2D.Double(getX(),getY());
		double distanceToTarget = myPos.distance(target.pos);
		double distanceToNextDestination = myPos.distance(nextDestination);

		if(distanceToNextDestination < 15) {
			// Avaliacao de posicoes
			Point2D.Double testPoint;
			double addLast = 1 - Math.rint(Math.pow(Math.random(), getOthers()));
			Rectangle2D.Double battleField = new Rectangle2D.Double(30, 30, getBattleFieldWidth() - 60, getBattleFieldHeight() - 60);
			for (int i = 0; i < 200; i++) {
				testPoint = MyUtils.calcPoint(myPos, Math.min(distanceToTarget * 0.8, 100 + 200 * Math.random()), 2 * Math.PI * Math.random());
				if(battleField.contains(testPoint) && evaluate(testPoint, addLast) < evaluate(nextDestination, addLast)) {
					nextDestination = testPoint;
				}
			}
			lastPosition = myPos;
		} else {
			// Movimentacao normal
			double direction = 1;
			double angle = MyUtils.calcAngle(nextDestination, myPos) - getHeadingRadians();

			if(Math.cos(angle) < 0) {
				angle += Math.PI;
				direction = -1;
			}

			setAhead(distanceToNextDestination * direction);
			setTurnRightRadians(angle = Utils.normalRelativeAngle(angle));
			// hitting walls isn't a good idea, but HawkOnFire still does it pretty often
			setMaxVelocity(Math.abs(angle) > 1 ? 0 : 8d);
		}
	}

	private double evaluate(Point2D.Double p, double addLast) {
		// this is basically here that the bot uses more space on the battlefield. In melee it is dangerous to stay somewhere too long.
		double eval = addLast * 0.08 / p.distanceSq(lastPosition);
 
 		Iterator it = enemies.values().iterator();
 		while(it.hasNext()) {
 			EnemyBot bot = (EnemyBot) it.next();
 			if (bot.energy > 0) {
				eval += Math.min(bot.energy / getEnergy(), 2) * (1 + Math.abs(Math.cos(MyUtils.calcAngle(myPos, p) - MyUtils.calcAngle(bot.pos, p)))) / p.distanceSq(bot.pos);
			}
 		}
		return eval;
	}

	private void shootLogic() {
		if ("".equals(target.name)) return;

		double firePower = Math.min(500 / target.distance, 3);
		double bulletSpeed = 20 - firePower * 3;

		long time = (long)(target.distance / bulletSpeed);

		// Calculando rotacao do canhão para as posicoes previstas (x,y)
		double absDeg = MyUtils.absoluteBearing(getX(), getY(), 
			target.getFutureX(time), target.getFutureY(time));

		setTurnGunRight(MyUtils.normalizeBearing(absDeg - getGunHeading()));
		if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
			setFire(firePower);
		}
	}

	public void onScannedRobot(ScannedRobotEvent evt) {
		EnemyBot enemyBot = (EnemyBot) enemies.get(evt.getName());
		
		if (enemyBot == null) {
			enemyBot = new EnemyBot();
			enemies.put(evt.getName(), enemyBot);
		}

		enemyBot.update(evt, this);
		enemyBot.pos = MyUtils.calcPoint(myPos, evt.getDistance(), 
			getHeadingRadians() + evt.getBearingRadians());

		// Se nao possui nenhum alvo ou o alvo está consideravelmente mais próximo
		if ("".equals(target.name) || evt.getDistance() < target.distance - 70) {
			target = enemyBot;
		}
	}

	public void onRobotDeath(RobotDeathEvent evt) {
		enemies.remove(evt.getName());
		target.reset();
	}
}