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
/**
 * 
 */
package com.fullmetalgalaxy.model.persist;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fullmetalgalaxy.model.Tide;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Serialized;
import com.googlecode.objectify.annotation.Unindexed;

/**
 * these data plus preview data contain all data needed to represent and play a game.
 * This class is used only to be stored into data base.
 * As we don't need to query on these data, it is unindexed and serialized.
 * 
 * @author vlegendr
 * TODO split into EbGameData and EbGameLog
 */
@Unindexed
public class EbGameData extends EbBase
{
  static final long serialVersionUID = 11;

  @Parent
  protected Key<EbGamePreview> m_preview;

  protected Tide m_currentTide = Tide.Medium;
  protected Tide m_nextTide = Tide.Medium;
  protected Tide m_nextTide2 = Tide.Medium;
  protected int m_lastTideChange = 0;
  protected Date m_lastTimeStepChange = new Date( System.currentTimeMillis() );
  protected ArrayList<Integer> m_takeOffTurns = null;
  protected String m_mapUri = null;

  /**
  * Land description. It's a two dimension array of landWitdh * landHeight
  */
  protected byte[] m_lands = new byte[0];


  // theses data come from other table
  // --------------------------------
  @Serialized
  protected Set<com.fullmetalgalaxy.model.persist.EbToken> m_setToken = new HashSet<com.fullmetalgalaxy.model.persist.EbToken>();
  @Serialized
  protected List<com.fullmetalgalaxy.model.persist.gamelog.AnEvent> m_setGameLog = new ArrayList<com.fullmetalgalaxy.model.persist.gamelog.AnEvent>();
  @Serialized
  protected List<com.fullmetalgalaxy.model.persist.triggers.EbTrigger> m_triggers = new ArrayList<com.fullmetalgalaxy.model.persist.triggers.EbTrigger>();

  
  protected long m_nextLocalId = 0L;

  /**
   * 
   */
  public EbGameData()
  {
    init();
  }

  /**
   * @param p_base
   */
  public EbGameData(EbBase p_base)
  {
    super( p_base );
    // TODO Auto-generated constructor stub
  }
  protected void init()
  {
    m_currentTide = Tide.Medium;
    m_nextTide = Tide.Medium;
    m_nextTide2 = Tide.Medium;
    m_lastTideChange = 0;
    m_lands = new byte[0];
    m_lastTimeStepChange = new Date( System.currentTimeMillis() );
    m_takeOffTurns = null;
    m_setToken = new HashSet<com.fullmetalgalaxy.model.persist.EbToken>();
    m_setGameLog = new ArrayList<com.fullmetalgalaxy.model.persist.gamelog.AnEvent>();
    m_triggers = new ArrayList<com.fullmetalgalaxy.model.persist.triggers.EbTrigger>();
    m_nextLocalId = 0L;
  }

  @Override
  public void reinit()
  {
    super.reinit();
    this.init();
  }

  
  public void setKeyPreview( Key<EbGamePreview> p_preview )
  {
    m_preview = p_preview;
  }
  
  
  /**
   * use this to generate any id local to this game.
   * @return
   */
  public long getNextLocalId()
  {
    m_nextLocalId++;
    return m_nextLocalId;
  }



  /**
   * Don't use this method directly, it's for hibernate and h4gwt use only
   * @return the lands
   * @WgtHidden
   */
  public byte[] getLands()
  {
    return m_lands;
  }

  /**
   * Don't use this method directly, it's for hibernate and h4gwt use only
   * @param p_lands the lands to set
   */
  public void setLands(byte[] p_lands)
  {
    m_lands = p_lands;
  }



  /**
   * @return the setToken
   * @WgtHidden
   */
  public Set<com.fullmetalgalaxy.model.persist.EbToken> getSetToken()
  {
    return m_setToken;
  }

  /**
   * @param p_setToken the setToken to set
   */
  public void setSetToken(Set<com.fullmetalgalaxy.model.persist.EbToken> p_setToken)
  {
    m_setToken = p_setToken;
  }




  /**
   * @return the currentTide
   * @WgtHidden
   */
  public Tide getCurrentTide()
  {
    return m_currentTide;
  }

  /**
   * @param p_currentTide the currentTide to set
   */
  public void setCurrentTide(Tide p_currentTide)
  {
    m_currentTide = p_currentTide;
  }



  /**
   * @return the nextTide
   * @WgtHidden
   */
  public Tide getNextTide()
  {
    if( m_nextTide == null )
    {
      m_nextTide = Tide.getRandom();
    }
    return m_nextTide;
  }

  /**
   * @param p_nextTide the nextTide to set
   */
  public void setNextTide(Tide p_nextTide)
  {
    m_nextTide = p_nextTide;
  }


  /**
   * @return the nextTide
   * @WgtHidden
   */
  public Tide getNextTide2()
  {
    if( m_nextTide2 == null )
    {
      m_nextTide2 = Tide.getRandom();
    }
    return m_nextTide2;
  }

  /**
   * @param p_nextTide the nextTide to set
   */
  public void setNextTide2(Tide p_nextTide)
  {
    m_nextTide2 = p_nextTide;
  }




  /**
   * @return the lastTideChange
   */
  public int getLastTideChange()
  {
    return m_lastTideChange;
  }

  /**
   * @param p_lastTideChange the lastTideChange to set
   */
  public void setLastTideChange(int p_lastTideChange)
  {
    m_lastTideChange = p_lastTideChange;
  }

  /**
   * @return the lastTimeStepChange
   */
  public Date getLastTimeStepChange()
  {
    return m_lastTimeStepChange;
  }

  /**
   * @param p_lastTimeStepChange the lastTimeStepChange to set
   */
  public void setLastTimeStepChange(Date p_lastTimeStepChange)
  {
    m_lastTimeStepChange = p_lastTimeStepChange;
  }


  /**
   * @return the setActionLog
   */
  public List<com.fullmetalgalaxy.model.persist.gamelog.AnEvent> getLogs()
  {
    return m_setGameLog;
  }

  /**
   * @param p_setActionLog the setActionLog to set
   */
  public void setLogs(List<com.fullmetalgalaxy.model.persist.gamelog.AnEvent> p_setActionLog)
  {
    m_setGameLog = p_setActionLog;
  }

  /**
   * use getAllowedTakeOffTurns() instead
   * @return the takeOffTurns
   */
  public ArrayList<Integer> getTakeOffTurns()
  {
    return m_takeOffTurns;
  }

  /**
   * @param p_takeOffTurns the takeOffTurns to set
   */
  public void setTakeOffTurns(ArrayList<Integer> p_takeOffTurns)
  {
    m_takeOffTurns = p_takeOffTurns;
  }

  /**
   * @return the triggers
   */
  public List<com.fullmetalgalaxy.model.persist.triggers.EbTrigger> getTriggers()
  {
    return m_triggers;
  }

  /**
   * @param p_triggers the triggers to set
   */
  public void setTriggers(List<com.fullmetalgalaxy.model.persist.triggers.EbTrigger> p_triggers)
  {
    m_triggers = p_triggers;
  }


  /**
   * @return the mapUri
   */
  public String getMapUri()
  {
    return m_mapUri;
  }

  /**
   * @param p_mapUri the mapUri to set
   */
  public void setMapUri(String p_mapUri)
  {
    m_mapUri = p_mapUri;
  }


}