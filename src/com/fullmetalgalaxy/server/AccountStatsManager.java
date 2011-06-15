/* *********************************************************************
 *
 *  This file is part of Full Metal Galaxy.
 *  http://www.fullmetalgalaxy.com
 *
 *  Full Metal Galaxy is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation, either version 3 of 
 *  the License, or (at your option) any later version.
 *
 *  Full Metal Galaxy is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public 
 *  License along with Full Metal Galaxy.  
 *  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2010, 2011 Vincent Legendre
 *
 * *********************************************************************/

package com.fullmetalgalaxy.server;

import java.util.Collections;
import java.util.Date;

import com.fullmetalgalaxy.model.constant.FmpConstant;
import com.fullmetalgalaxy.model.persist.EbAccountStats;
import com.fullmetalgalaxy.model.persist.EbRegistration;
import com.fullmetalgalaxy.model.persist.Game;
import com.fullmetalgalaxy.model.persist.PlayerFiability;
import com.fullmetalgalaxy.model.persist.PlayerStyle;
import com.fullmetalgalaxy.model.persist.StatsErosion;
import com.fullmetalgalaxy.model.persist.StatsGame;
import com.fullmetalgalaxy.model.persist.StatsGame.Status;
import com.fullmetalgalaxy.model.persist.StatsGamePlayer;

/**
 * Update account statistic according to some event
 *
 * erosion decrease player point according to time to allow new player becoming on top of ranking
 * cf erosion.xls for erosion test and formulas
 * 
 * @author Vincent
 *
 */
public class AccountStatsManager
{
  // coef of erosion polynom formulas
  private final static float EROSION_A = -1 * FmpConstant.SCORE_EROSION_REF
      * FmpConstant.SCORE_EROSION_REF
      / (4 * (FmpConstant.SCORE_EROSION_MIN - FmpConstant.SCORE_REF));
  private final static int EROSION_B = FmpConstant.SCORE_EROSION_REF;
  private final static int EROSION_C = FmpConstant.SCORE_REF;
  private final static float EROSION_TIME_MIN = -1 * EROSION_B / (2 * EROSION_A);

  private final static int MONTH_IN_MILLIS = 1000 * 60 * 60 * 24 * 30;


  /**
   * @param p_time unit is one month
   * @return point
   */
  private static float polynom(float p_time)
  {
    return EROSION_A * p_time * p_time + EROSION_B * p_time + EROSION_C;
  }

  /**
   * @param p_point
   * @return time in month
   */
  private static float polynom_inv(float p_point)
  {
    return (float)(-1
        * Math.sqrt( Math.abs( p_point / EROSION_A
            + (EROSION_B * EROSION_B - 4 * EROSION_A * EROSION_C) / (4 * EROSION_A * EROSION_A) ) ) - EROSION_B
        / (2 * EROSION_A));
  }

  /**
   * According to an amount of point and a duration, compute the new players points
   * @param p_point
   * @param p_durationInMillis
   * @return
   */
  private static int erosion(int p_point, long p_durationInMillis)
  {
    float months = p_durationInMillis / MONTH_IN_MILLIS;
    float time = polynom_inv( p_point );
    time += months;
    if( time >= EROSION_TIME_MIN )
    {
      return FmpConstant.SCORE_EROSION_MIN;
    }
    return ((int)polynom( time )) + 1;
  }



  /**
   *  may differ from oreCount + tokenCount as ore may have different value.
   * More important, this score depend of other players level !
   *
   * finalScore = (fmpScore - 20)*(sum(otherLevel)/(myLevel*otherPlayerCount))^sign(fmpScore) 
   *
   * for winner, we also add the sum of other players bonus
   * @param p_game
   * @param p_registration
   * @return
   */
  private static int processFinalScore(Game p_game, EbRegistration p_registration)
  {
    // process normal fmp score
    int fmpScore = p_registration.getWinningScore( p_game );

    // process level ratio and bonus
    float levelRatio = 0;
    int otherPlayerCount = 0;
    for( EbRegistration registration : p_game.getSetRegistration() )
    {
      if( registration != p_registration && registration.getAccount() != null )
      {
        levelRatio += registration.getAccount().getCurrentLevel();
        otherPlayerCount++;
      }
    }
    levelRatio /= p_registration.getAccount().getCurrentLevel() * otherPlayerCount;

    // process final score
    int finalScore = 0;
    if( fmpScore > 0 )
    {
      finalScore = (int)Math.round( fmpScore * levelRatio );
    }
    else if( fmpScore < 0 )
    {
      finalScore = (int)Math.round( fmpScore / levelRatio );
    }
    if( p_game.getWinnerRegistration() == p_registration )
    {
      finalScore += p_game.getScoreBonus() - p_registration.getAccount().getScoreBonus();
    }

    return finalScore;
  }

  private static StatsGame getLastStats(EbAccount p_account, long p_gameId)
  {
    if( p_account.getStats() == null || p_account.getStats().isEmpty() )
    {
      return null;
    }
    for( int i = p_account.getStats().size() - 1; i >= 0; i-- )
    {
      EbAccountStats stat = p_account.getStats().get( i );
      if( stat instanceof StatsGame )
      {
        if( ((StatsGame)stat).getGameId() == p_gameId )
        {
          return (StatsGame)stat;
        }
      }
    }
    return null;
  }


  private static StatsErosion getLastErosion(EbAccount p_account)
  {
    if( p_account.getStats() == null || p_account.getStats().isEmpty() )
    {
      return null;
    }
    int index = p_account.getStats().size();
    while( index > 0 )
    {
      index--;
      EbAccountStats stat = p_account.getStats().get( index );
      if( stat instanceof StatsErosion )
      {
        return (StatsErosion)stat;
      }
    }
    return null;
  }


  private static EbAccountStats getLastFixedStats(EbAccount p_account)
  {
    if( p_account.getStats() == null || p_account.getStats().isEmpty() )
    {
      return null;
    }
    int index = p_account.getStats().size();
    while( index > 0 )
    {
      index--;
      EbAccountStats stat = p_account.getStats().get( index );
      if( !stat.lastUpdateCanChange() )
      {
        return stat;
      }
    }
    return null;
  }


  @SuppressWarnings("unchecked")
  private static void saveAndUpdate(EbAccount p_account)
  {
    // sort stat according to their date
    Collections.sort( p_account.getStats() );
    // update player level and other style
    int level = 1;
    int banCount = 0;
    int sheepCount = 0;
    float style = 0;
    int styleCount = 0;
    for( EbAccountStats stat : p_account.getStats() )
    {
      level += stat.getFinalScore();
      if( level <= 0 )
      {
        level = 1;
      }
      if( stat instanceof StatsGamePlayer )
      {
        if( ((StatsGamePlayer)stat).getStatus() == Status.Banned )
        {
          banCount++;
        }
        else if( ((StatsGamePlayer)stat).getStatus() == Status.Finished )
        {
          PlayerStyle playerStyle = ((StatsGamePlayer)stat).getPlayerStyle();
          switch( playerStyle )
          {
          case Sheep:
            sheepCount++;
            break;
          case Pacific:
            style--;
            styleCount++;
            break;
          default:
          case Balanced:
            styleCount++;
            break;
          case Aggressive:
            style++;
            styleCount++;
            break;
          }
        }
      }
    }
    p_account.setCurrentLevel( level );
    if( banCount > styleCount + sheepCount )
    {
      p_account.setFiability( PlayerFiability.Banned );
    }
    if( sheepCount > styleCount )
    {
      p_account.setPlayerStyle( PlayerStyle.Sheep );
    }
    else
    {
      style /= styleCount;
      if( style < -0.35 )
      {
        p_account.setPlayerStyle( PlayerStyle.Pacific );
      }
      else if( style > 0.35 )
      {
        p_account.setPlayerStyle( PlayerStyle.Aggressive );
      }
      else
      {
        p_account.setPlayerStyle( PlayerStyle.Balanced );
      }
    }
    // save account to datastore
    FmgDataStore ds = new FmgDataStore( false );
    ds.put( p_account );
    ds.close();
  }

  public static void gameCreate(EbAccount p_account, Game p_game)
  {
    StatsGame stat = getLastStats( p_account, p_game.getId() );
    if( stat == null )
    {
      // stat is likely to be null as game is just created
      stat = new StatsGame( p_game );
      stat.setCreator( true );
      p_account.getStats().add( stat );
    }
    stat.setLastUpdate( new Date() );
    
    saveAndUpdate( p_account );
  }

  public static void gameJoin(EbAccount p_account, Game p_game)
  {
    StatsGamePlayer lastStat = new StatsGamePlayer( p_game );
    p_account.getStats().add( lastStat );
    EbRegistration registration = p_game.getRegistrationByIdAccount( p_account.getId() );
    lastStat.setPlayer( p_game, registration );
    saveAndUpdate( p_account );
  }

  public static void gameBan(EbAccount p_account, Game p_game)
  {
    StatsGamePlayer lastStat = StatsGamePlayer.class.cast( getLastStats( p_account, p_game.getId() ) );
    if( lastStat == null )
    {
      lastStat = new StatsGamePlayer( p_game );
      p_account.getStats().add( lastStat );
    }
    
    lastStat.setStatus( Status.Banned );
    lastStat.setFinalScore( 0 - p_game.getEbConfigGameVariant().getInitialScore()
        - p_account.getScoreBonus() );
    lastStat.setLastUpdate( new Date() );
    
    // no this is useless as account is banned...
    // EbRegistration registration = p_game.getRegistrationByIdAccount( p_account.getId() );
    // lastStat.setPlayer( p_game, registration );

    saveAndUpdate( p_account );
  }

  public static void gameFinish(Game p_game)
  {
    // for player
    for( EbRegistration registration : p_game.getSetRegistration() )
    {
      if( registration.getAccount() != null )
      {
        EbAccount account = FmgDataStore.dao().get( EbAccount.class,
            registration.getAccount().getId() );
        StatsGamePlayer lastStat = StatsGamePlayer.class.cast( getLastStats( account,
            p_game.getId() ) );
        if( lastStat == null )
        {
          lastStat = new StatsGamePlayer( p_game );
          account.getStats().add( lastStat );
        }
        lastStat.setPlayer( p_game, registration );
        lastStat.setStatus( Status.Finished );
        lastStat.setFinalScore( processFinalScore( p_game, registration ) );
        lastStat.setLastUpdate( new Date() );
        saveAndUpdate( account );
      }
    }

    // for creator
    EbAccount account = FmgDataStore.dao()
        .get( EbAccount.class, p_game.getAccountCreator().getId() );
    StatsGame lastStat = StatsGame.class.cast( getLastStats( account, p_game.getId() ) );
    if( lastStat == null )
    {
      lastStat = new StatsGame( p_game );
      account.getStats().add( lastStat );
    }
    lastStat.setStatus( Status.Finished );
    lastStat.setLastUpdate( new Date() );
    saveAndUpdate( account );
  }


  public static void gameAbort(Game p_game)
  {
    // for player
    for( EbRegistration registration : p_game.getSetRegistration() )
    {
      if( registration.getAccount() != null )
      {
        EbAccount account = FmgDataStore.dao().get( EbAccount.class,
            registration.getAccount().getId() );
        StatsGamePlayer lastStat = StatsGamePlayer.class.cast( getLastStats( account,
            p_game.getId() ) );
        if( lastStat == null )
        {
          lastStat = new StatsGamePlayer( p_game );
          lastStat.setPlayer( p_game, registration );
          account.getStats().add( lastStat );
        }
        lastStat.setStatus( Status.Aborted );
        lastStat.setFinalScore( 0 );
        lastStat.setLastUpdate( new Date() );
        saveAndUpdate( account );
      }
    }

    // for creator
    EbAccount account = FmgDataStore.dao()
        .get( EbAccount.class, p_game.getAccountCreator().getId() );
    StatsGame lastStat = StatsGame.class.cast( getLastStats( account, p_game.getId() ) );
    if( lastStat == null )
    {
      lastStat = new StatsGame( p_game );
      account.getStats().add( lastStat );
    }
    lastStat.setStatus( Status.Aborted );
    lastStat.setFinalScore( 0 );
    lastStat.setLastUpdate( new Date() );
    saveAndUpdate( account );

  }

  public static void erosion(EbAccount p_account)
  {
    int oldPoint = p_account.getCurrentLevel();
    int newPoint = oldPoint;
    EbAccountStats lastFixedStat = getLastFixedStats( p_account );
    StatsErosion lastErosion = getLastErosion( p_account );

    if( lastErosion == null )
    {
      lastErosion = new StatsErosion();
      p_account.getStats().add( lastErosion );
    }
    else if( lastErosion == lastFixedStat )
    {
      // add erosion with existing previous erosion
      oldPoint = p_account.getCurrentLevel() - lastErosion.getFinalScore();
      newPoint = erosion( oldPoint, System.currentTimeMillis()
          - lastErosion.getFromDate().getTime() );
    }
    else
    {
      oldPoint = p_account.getCurrentLevel();
      newPoint = erosion( oldPoint, System.currentTimeMillis()
          - lastFixedStat.getLastUpdate().getTime() );
      lastErosion = new StatsErosion();
      p_account.getStats().add( lastErosion );
    }

    lastErosion.setLastUpdate( new Date() );
    lastErosion.setFinalScore( newPoint - oldPoint );

    saveAndUpdate( p_account );
  }


}