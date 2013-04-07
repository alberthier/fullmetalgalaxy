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
 *  Copyright 2010, 2011, 2012, 2013 Vincent Legendre
 *
 * *********************************************************************/
package com.fullmetalgalaxy.model.persist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Embedded;

import com.fullmetalgalaxy.model.EnuColor;
import com.fullmetalgalaxy.model.Location;
import com.fullmetalgalaxy.model.SharedMethods;
import com.fullmetalgalaxy.model.TokenType;
import com.fullmetalgalaxy.model.constant.FmpConstant;
import com.fullmetalgalaxy.model.persist.gamelog.AnEvent;
import com.googlecode.objectify.annotation.Serialized;


/**
 * @author Kroc
 * represent an association between an account (or user) and a game. so this account become
 * a player for this game
 */
public class EbRegistration extends EbBase
{
  static final long serialVersionUID = 1;

  public EbRegistration()
  {
    super();
    init();
  }

  public EbRegistration(EbBase p_base)
  {
    super( p_base );
    init();
  }


  private void init()
  {
    m_color = EnuColor.None;
    m_ptAction = 0;
    m_orderIndex = 0;
    m_originalAccountId = 0;
  }

  @Override
  public void reinit()
  {
    super.reinit();
    this.init();
  }

  @Override
  public String toString()
  {
    return new EnuColor( getSingleColor() ) + "(" + getAccount() + ")";
  }


  // theses data come from database (Game table)
  // ------------------------------------------
  private int m_color = EnuColor.Unknown;
  private int m_OriginalColor = EnuColor.Unknown;
  private int m_singleColor = EnuColor.Unknown;
  private int m_ptAction = 0;
  private int m_orderIndex = 0;
  /** number of weather hen at the last time step change. */
  private int m_workingWeatherHenCount = 0;

  /** in turn by turn mode this is the end turn date.
   * and in parallel mode, this is the date up to which the board is locked (cf m_lockedPosition) */
  private Date m_endTurnDate = null;
  /** in parallel mode, player lock a board area for a small period */
  @Serialized
  private AnBoardPosition m_lockedPosition = null;

  private Date m_lastConnexion = new Date();
  @Serialized
  private List<String> m_notifSended = null;
  private long m_originalAccountId = 0;

  @Embedded
  private EbPublicAccount m_account = null;

  @Serialized
  private StatsPlayer m_stats = null;

  /**
   * action list that player made during a parallel and hidden turn (ie deployement or take off turn)
   * these actions are seen by player but hidden to others. This event list will be merged with main log
   * at the end of current turn.
   */
  @Serialized
  protected List<AnEvent> m_myEvents = new ArrayList<AnEvent>();



  public int getOreCount(Game p_game)
  {
    int count = 0;
    for( EbToken token : p_game.getSetToken() )
    {
      if( (token.getType().isOre()) && (token.getCarrierToken() != null)
          && (token.getCarrierToken().getType() == TokenType.Freighter)
          && getEnuColor().isColored( token.getCarrierToken().getColor() ) )
      {
        count += 1;
      }
    }
    return count;
  }

  public int getTokenCount(Game p_game)
  {
    int count = 0;
    for( EbToken token : p_game.getSetToken() )
    {
      if( token.getEnuColor().isSingleColor() && getEnuColor().isColored( token.getColor() )
          && token.getType() != TokenType.Freighter && token.getType() != TokenType.Turret )
      {
        count++;
      }
    }
    return count;
  }

  /**
   * if game is finished, return final score
   * @param p_game
   * @return
   */
  public int estimateWinningScore(Game p_game)
  {
    int winningPoint = 0;
    if( p_game.isFinished() )
    {
      for( EbToken token : p_game.getSetToken() )
      {
        if( (token.getType() == TokenType.Freighter) && getEnuColor().isColored( token.getColor() )
            && (token.getLocation() == Location.EndGame) )
        {
          winningPoint += token.getWinningPoint();
        }
      }
    }
    else
    {
      for( EbToken token : p_game.getSetToken() )
      {
        if( (token.getColor() != EnuColor.None) && (getEnuColor().isColored( token.getColor() ))
            && (token.getLocation() == Location.Board || token.getLocation() == Location.EndGame) )
        {
          winningPoint += token.getWinningPoint();
        }
      }
    }
    winningPoint -= FmpConstant.initialScore;
    return winningPoint;
  }



  /**
   * @return the account
   */
  public boolean haveAccount()
  {
    return getAccount() != null && getAccount().getId() > 0L;
  }



  /**
   * like getActionPt() but after been rounded according to selected time variant.
   * @return
   */
  public int getRoundedActionPt(Game p_game)
  {
    int futurActionPt = getPtAction() / p_game.getEbConfigGameTime().getRoundActionPt();
    futurActionPt *= p_game.getEbConfigGameTime().getRoundActionPt();
    if( futurActionPt > p_game.getEbConfigGameTime().getActionPtMaxReserve() - 15 )
    {
      futurActionPt = p_game.getEbConfigGameTime().getActionPtMaxReserve() - 15;
    }
    return futurActionPt;
  }

  public int getMaxActionPt(Game p_game)
  {
    int freighterCount = getOnBoardFreighterCount( p_game );
    return p_game.getEbConfigGameTime().getActionPtMaxReserve()
        + ((freighterCount - 1) * p_game.getEbConfigGameTime().getActionPtMaxPerExtraShip());
  }


  private static int getDefaultActionInc(Game p_game)
  {
    int timeStep = p_game.getCurrentTimeStep();
    timeStep -= p_game.getEbConfigGameTime().getDeploymentTimeStep();
    int actionInc = p_game.getEbConfigGameTime().getActionPtPerTimeStep();

    if( timeStep <= 0 )
    {
      actionInc = 0;
    }
    else if( timeStep == 1 )
    {
      actionInc = actionInc / 3;
    }
    else if( timeStep == 2 )
    {
      actionInc = (2 * actionInc) / 3;
    }
    return actionInc;
  }


  public int getOnBoardFreighterCount(Game p_game)
  {
    int freighterCount = getEnuColor().getNbColor();
    // after turn 21, we really count number of landed freighter
    if( p_game.getCurrentTimeStep() >= p_game.getEbConfigGameTime().getTakeOffTurns().get( 0 ) )
    {
      freighterCount = 0;
      for( EbToken freighter : p_game.getAllFreighter( this ) )
      {
        if( freighter.getLocation() == Location.Board )
          freighterCount++;
      }
    }
    return freighterCount;
  }

  public int getActionInc(Game p_game)
  {
    int action = 0;
    int freighterCount = getOnBoardFreighterCount( p_game );
    if( freighterCount >= 1 )
    {
      action += getDefaultActionInc( p_game );
      action += (freighterCount - 1) * p_game.getEbConfigGameTime().getActionPtPerExtraShip();
    }
    return action;
  }

  public boolean isNotifSended(String p_msgName)
  {
    if( m_notifSended == null )
    {
      return false;
    }
    return m_notifSended.contains( p_msgName );
  }

  public void clearNotifSended()
  {
    m_notifSended = null;
  }

  public void addNotifSended(String p_msgName)
  {
    if( m_notifSended == null )
    {
      m_notifSended = new ArrayList<String>();
    }
    m_notifSended.add( p_msgName );
  }

  // getters / setters
  // -----------------
  public void setEnuColor(EnuColor p_color)
  {
    m_color = p_color.getValue();
  }

  public EnuColor getEnuColor()
  {
    return new EnuColor( getColor() );
  }

  /**
   * @return the bitfield Color
   */
  public int getColor()
  {
    return m_color;
  }

  /**
   * @return the ptAction
   */
  public int getPtAction()
  {
    return m_ptAction;
  }

  /**
   * @param p_ptAction the ptAction to set
   */
  public void setPtAction(int p_ptAction)
  {
    m_ptAction = p_ptAction;
  }



  /**
   * @param p_color the color to set
   */
  public void setColor(int p_color)
  {
    setEnuColor( new EnuColor( p_color ) );
  }


  /**
   * @return the orderIndex
   */
  public int getOrderIndex()
  {
    return m_orderIndex;
  }

  /**
   * @param p_orderIndex the orderIndex to set
   */
  public void setOrderIndex(int p_orderIndex)
  {
    m_orderIndex = p_orderIndex;
  }

  /**
   * @return the color of his fire cover
   */
  public int getSingleColor()
  {
    // this test is here for backward compatibility
    if( (m_singleColor == EnuColor.Unknown || m_singleColor == EnuColor.None)
        && getColor() != EnuColor.None )
    {
      m_singleColor = getOriginalColor();
    }
    return m_singleColor;
  }

  /**
   * @param p_singleColor the originalColor to set
   */
  public void setSingleColor(int p_singleColor)
  {
    m_singleColor = p_singleColor;
  }

  /**
   * @return the originalColor
   */
  public int getOriginalColor()
  {
    return m_OriginalColor;
  }

  /**
   * @param p_originalColor the originalColor to set
   */
  public void setOriginalColor(int p_originalColor)
  {
    m_OriginalColor = p_originalColor;
  }



  /**
   * @return the endTurnDate
   */
  public Date getEndTurnDate()
  {
    return m_endTurnDate;
  }

  /**
   * Warning, it's recommended to use EbGame.setEndTurnDate instead of this one.
   * @param p_endTurnDate the endTurnDate to set
   */
  public void setEndTurnDate(Date p_endTurnDate)
  {
    m_endTurnDate = p_endTurnDate;
  }




  /**
   * @return the account
   */
  public EbPublicAccount getAccount()
  {
    return m_account;
  }

  /**
   * @param p_account the account to set
   */
  public void setAccount(EbPublicAccount p_account)
  {
    m_account = p_account;
  }


  public int getWorkingWeatherHenCount()
  {
    return m_workingWeatherHenCount;
  }

  public void setWorkingWeatherHenCount(int p_workingWeatherHenCount)
  {
    m_workingWeatherHenCount = p_workingWeatherHenCount;
  }

  /**
   * @return the lastConnexion
   */
  public Date getLastConnexion()
  {
    return m_lastConnexion;
  }

  /**
   * @param p_lastConnexion the lastConnexion to set
   */
  public void updateLastConnexion()
  {
    if( m_lastConnexion == null )
    {
      m_lastConnexion = new Date();
    }
    m_lastConnexion.setTime( SharedMethods.currentTimeMillis() );
  }

  /**
   * @return the isReplacement
   */
  public boolean isReplacement()
  {
    return m_originalAccountId != 0;
  }


  public StatsPlayer getStats()
  {
    return m_stats;
  }

  public void setStats(StatsPlayer p_stats)
  {
    m_stats = p_stats;
  }

  public AnBoardPosition getLockedPosition()
  {
    return m_lockedPosition;
  }

  public void setLockedPosition(AnBoardPosition p_lockedPosition)
  {
    m_lockedPosition = p_lockedPosition;
  }

  public long getOriginalAccountId()
  {
    return m_originalAccountId;
  }

  public EbPublicAccount getOriginalAccount(Game p_game)
  {
    return p_game.getAccount( getOriginalAccountId() );
  }

  public void setOriginalAccountId(long p_originalAccountId)
  {
    m_originalAccountId = p_originalAccountId;
  }


  /**
   * don't use this method to add event !
   * @return a read only list
   */
  public List<AnEvent> getMyEvents()
  {
    if( m_myEvents == null )
    {
      return new ArrayList<AnEvent>();
    }
    return m_myEvents;
  }

  public void setMyEvents(List<AnEvent> p_myEvents)
  {
    m_myEvents = p_myEvents;
  }

  public void clearMyEvents()
  {
    m_myEvents = null;
  }

  public void addMyEvent(AnEvent p_action)
  {
    if( m_myEvents == null )
    {
      m_myEvents = new ArrayList<AnEvent>();
    }
    if( !m_myEvents.contains( p_action ) )
    {
      m_myEvents.add( p_action );
    }
  }

}
