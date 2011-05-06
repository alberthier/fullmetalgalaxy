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
package com.fullmetalgalaxy.model.persist;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.Embedded;
import javax.persistence.PrePersist;

import com.fullmetalgalaxy.model.EnuColor;
import com.fullmetalgalaxy.model.EnuZoom;
import com.fullmetalgalaxy.model.GameType;
import com.fullmetalgalaxy.model.LandType;
import com.fullmetalgalaxy.model.PlanetType;
import com.fullmetalgalaxy.model.constant.ConfigGameTime;
import com.fullmetalgalaxy.model.constant.ConfigGameVariant;
import com.fullmetalgalaxy.model.constant.FmpConstant;
import com.googlecode.objectify.annotation.Serialized;
import com.googlecode.objectify.annotation.Unindexed;

/**
 * @author Kroc
 * This class represent a preview for game model.
 * It is used to display in a game list and to make query in database.
 */
public class EbGamePreview extends EbBase
{
  static final long serialVersionUID = 11;

  @Unindexed
  private long m_version = 0;
  private String m_name = "";
  @Unindexed
  private String m_description = "";
  private int m_maxNumberOfPlayer = 0;
  private boolean m_history = false;
  @Unindexed
  private int m_currentTimeStep = 0;
  private GameType m_gameType = GameType.MultiPlayer;
  private PlanetType m_planetType = PlanetType.Desert;
  private boolean m_isOpen = true;
  /** minimap stored here to save some ImageService's CPU time */
  @Unindexed
  private String m_minimapBlobKey = null;
  @Unindexed
  private String m_minimapUri = null;

  /** registration ID */
  @Unindexed
  private long m_currentPlayerId = 0L;

  @Embedded
  private EbPublicAccount m_accountCreator = null;

  private int m_landWidth = 0;
  private int m_landHeight = 0;
  private Date m_creationDate = new Date( System.currentTimeMillis() );
  private boolean m_started = false;

  private Date m_lastUpdate = new Date();

  /**
   * only VIP people can join VIP game.
   */
  private boolean m_isVip = false;
  
  // configuration
  private ConfigGameTime m_configGameTime = ConfigGameTime.Standard;
  private ConfigGameVariant m_configGameVariant = ConfigGameVariant.Standard;
  @Serialized
  private EbConfigGameTime m_ebConfigGameTime = null;
  @Serialized
  private EbConfigGameVariant m_ebConfigGameVariant = null;

  @Embedded
  private Set<com.fullmetalgalaxy.model.persist.EbRegistration> m_setRegistration = new HashSet<com.fullmetalgalaxy.model.persist.EbRegistration>();

  
  public EbGamePreview()
  {
    super();
    init();
  }



  private void init()
  {
    m_maxNumberOfPlayer = 0;
    m_history = false;
    m_currentTimeStep = 0;
    m_currentPlayerId = 0L;
    m_landWidth = 0;
    m_landHeight = 0;
    m_creationDate = new Date( System.currentTimeMillis() );
    m_description = "";
    m_name = "";
    m_gameType = GameType.MultiPlayer;
    m_planetType = PlanetType.Desert;
    m_setRegistration = new HashSet<com.fullmetalgalaxy.model.persist.EbRegistration>();
  }

  @Override
  public void reinit()
  {
    super.reinit();
    this.init();
  }

  @PrePersist
  void onPersist()
  { /* do something before persisting */
    getLastUpdate().setTime( System.currentTimeMillis() );
  }


  public int getLandPixWidth(EnuZoom p_zoom)
  {
    return getLandWidth() * ((FmpConstant.getHexWidth( p_zoom ) * 3) / 4)
        + FmpConstant.getHexWidth( p_zoom ) / 4;
  }

  public int getLandPixHeight(EnuZoom p_zoom)
  {
    return(getLandHeight() * FmpConstant.getHexHeight( p_zoom ) + FmpConstant.getHexHeight( p_zoom ) / 2);
  }


  /**
   * @return the currentPlayerRegistration
   * @WgtHidden
   */
  public EbRegistration getCurrentPlayerRegistration()
  {
    return getRegistration( m_currentPlayerId );
  }

  /**
   * @param p_currentPlayerRegistration the currentPlayerRegistration to set
   */
  public void setCurrentPlayerRegistration(EbRegistration p_currentPlayerRegistration)
  {
    if( p_currentPlayerRegistration == null )
    {
      m_currentPlayerId = 0L;
    }
    else
    {
      m_currentPlayerId = p_currentPlayerRegistration.getId();
    }
  }





  /**
   * @return null if p_idRegistration doesn't exist
   */
  public EbRegistration getRegistration(long p_idRegistration)
  {
    for( Iterator<EbRegistration> it = getSetRegistration().iterator(); it.hasNext(); )
    {
      EbRegistration registration = (EbRegistration)it.next();
      if( registration.getId() == p_idRegistration )
      {
        return registration;
      }
    }
    return null;
  }

  /**
   * @return null if p_index doesn't exist
   */
  public EbRegistration getRegistrationByOrderIndex(int p_index)
  {
    for( Iterator<EbRegistration> it = getSetRegistration().iterator(); it.hasNext(); )
    {
      EbRegistration registration = (EbRegistration)it.next();
      if( registration.getOrderIndex() == p_index )
      {
        return registration;
      }
    }
    return null;
  }

  /**
   * @return null if p_index doesn't exist
   */
  public EbRegistration getRegistrationByIdAccount(long p_idAccount)
  {
    for( Iterator<EbRegistration> it = getSetRegistration().iterator(); it.hasNext(); )
    {
      EbRegistration registration = (EbRegistration)it.next();
      if( registration.haveAccount() && registration.getAccount().getId() == p_idAccount )
      {
        return registration;
      }
    }
    return null;
  }


  /**
   * @return the registration which control p_color. (or null if it doesn't exist)
   */
  public EbRegistration getRegistrationByColor(int p_color)
  {
    for( Iterator<EbRegistration> it = getSetRegistration().iterator(); it.hasNext(); )
    {
      EbRegistration registration = (EbRegistration)it.next();
      if( registration.getEnuColor().isColored( p_color ) )
      {
        return registration;
      }
    }
    return null;
  }


  public List<EbRegistration> getRegistrationByPlayerOrder()
  {
    List<EbRegistration> sortedRegistration = new ArrayList<EbRegistration>();
    // sort registration according to their order index.
    for( EbRegistration registration : getSetRegistration() )
    {
      int index = 0;
      while( index < sortedRegistration.size() )
      {
        if( registration.getOrderIndex() < sortedRegistration.get( index ).getOrderIndex() )
        {
          break;
        }
        index++;
      }
      sortedRegistration.add( index, registration );
    }

    return sortedRegistration;
  }




  public EbRegistration getPreviousPlayerRegistration()
  {
    EbRegistration registration = null;
    int index = getCurrentPlayerRegistration().getOrderIndex();
    do
    {
      index--;
      if( index < 0 )
      {
        index = getCurrentNumberOfRegiteredPlayer() - 1;
      }
      registration = getRegistrationByOrderIndex( index );
      assert registration != null;
    } while( registration.getColor() == EnuColor.None );
    return registration;
  }




  /**
   * offset height in pixel to display token image in tactic zoom.
   * it represent the land height.
   * do not take in account tide
   */
  public static int getLandPixOffset(LandType p_landValue)
  {
    switch( p_landValue )
    {
    case Montain:
      return -7;
    case Sea:
      return 6;
    case Reef:
      return 5;
    case Marsh:
      return 2;
    default:
      return 0;
    }
  }


 

  /**
   * @return a set of free (not controlled by any registration, account may be null)
   *  single color
   */
  public Set<EnuColor> getFreeColors4Registration()
  {
    Set<EnuColor> colors = new HashSet<EnuColor>();
    EnuColor takenColor = new EnuColor( EnuColor.None );
    for( EbRegistration registration : getSetRegistration() )
    {
      takenColor.addColor( registration.getColor() );
    }
    for( int iColor = 0; iColor < EnuColor.getTotalNumberOfColor(); iColor++ )
    {
      EnuColor color = EnuColor.getColorFromIndex( iColor );
      if( !takenColor.isColored( color ) )
      {
        colors.add( color );
      }
    }
    return colors;
  }

  /**
   * @return a set of free (controled by registration with null account)
   *  single color
   */
  public Set<EnuColor> getFreeRegistrationColors()
  {
    Set<EnuColor> colors = new HashSet<EnuColor>();
    for( EbRegistration registration : getSetRegistration() )
    {
      if( !registration.haveAccount() )
      {
        colors.add( registration.getEnuColor() );
      }
    }
    return colors;
  }

  /**
   * @return a set of free (not controlled by any registration with an associated account)
   *  single color
   */
  public Set<EnuColor> getFreePlayersColors()
  {
    Set<EnuColor> colors = new HashSet<EnuColor>();
    EnuColor takenColor = new EnuColor( EnuColor.None );
    for( EbRegistration registration : getSetRegistration() )
    {
      if( registration.haveAccount() )
      {
        takenColor.addColor( registration.getColor() );
      }
    }
    for( int iColor = 0; iColor < EnuColor.getTotalNumberOfColor(); iColor++ )
    {
      EnuColor color = EnuColor.getColorFromIndex( iColor );
      if( !takenColor.isColored( color ) )
      {
        colors.add( color );
      }
    }
    return colors;
  }


  public String getPlayersAsString()
  {
    StringBuffer strBuf = new StringBuffer( " " );
    // get player order
    List<EbRegistration> sortedRegistration = new ArrayList<EbRegistration>();
    for( int index = 0; index < getSetRegistration().size(); index++ )
    {
      sortedRegistration.add( getRegistrationByOrderIndex( index ) );
    }

    int playerCount = 0;
    for( EbRegistration player : sortedRegistration )
    {
      playerCount++;
      if(player != null && player.haveAccount() )
      {
        strBuf.append( player.getAccount().getPseudo() );
      }
      if( (!isAsynchron() || getCurrentTimeStep() == 0)
          && (getCurrentPlayerRegistration() == player) )
      {
        strBuf
            .append( " <img style='border=none' border=0 src='/images/css/icon_action.cache.png' alt='Current' />" );
      }
      if( playerCount < sortedRegistration.size() )
      {
        if( isAsynchron() )
        {
          strBuf.append( " - " );
        }
        else
        {
          strBuf.append( " > " );
        }
      }
    }
    strBuf.append( " " );
    return strBuf.toString();
  }



  /**
   * update isOpen flag (for future query)
   * @return true if a player can join the game.
   */

  public boolean isOpen()
  {
    m_isOpen = ((getCurrentNumberOfRegiteredPlayer() < getMaxNumberOfPlayer()) && (!isStarted()) && !isHistory());
    return m_isOpen;
  }


  /**
   * @return the Number Of Registration associated with an account
   * @WgtHidden
   */

  public int getCurrentNumberOfRegiteredPlayer()
  {
    int count = 0;
    for( EbRegistration registration : getSetRegistration() )
    {
      if( registration.haveAccount() )
      {
        count++;
      }
    }
    return count;
  }




  /**
   * @return the account
   */
  public EbPublicAccount getAccountCreator()
  {
    return m_accountCreator;
  }

  /**
   * @param p_account the account to set
   */
  public void setAccountCreator(EbPublicAccount p_account)
  {
    m_accountCreator = p_account;
  }

  /**
   * @return the creationDate
   * @WgtHidden
   */
  public Date getCreationDate()
  {
    return m_creationDate;
  }

  /**
   * @param p_creationDate the creationDate to set
   */
  public void setCreationDate(Date p_creationDate)
  {
    m_creationDate = p_creationDate;
  }


  /**
   * @return the landWitdh in hexagon
   */
  public int getLandWidth()
  {
    return m_landWidth;
  }

  /**
   * @param p_landWidth the landWitdh to set
   * Warning: this method do not change the lands blobs size. use setLandSize(int,int) instead.
   */
  public void setLandWidth(int p_landWidth)
  {
    m_landWidth = p_landWidth;
  }

  /**
   * @return the landHeight in hexagon
   */
  public int getLandHeight()
  {
    return m_landHeight;
  }

  /**
   * @param p_landHeight the landHeight to set
   * Warning: this method do not change the lands blobs size. use setLandSize(int,int) instead.
   */
  public void setLandHeight(int p_landHeight)
  {
    m_landHeight = p_landHeight;
  }


  /**
   * @return the setRegistration
   * @WgtHidden
   */
  public Set<com.fullmetalgalaxy.model.persist.EbRegistration> getSetRegistration()
  {
    return m_setRegistration;
  }

  /**
   * @param p_setRegistration the setRegistration to set
   */
  public void setSetRegistration(
      Set<com.fullmetalgalaxy.model.persist.EbRegistration> p_setRegistration)
  {
    m_setRegistration = p_setRegistration;
  }


  /**
   * @return the name
   * @WgtRequired
   */
  public String getName()
  {
    return m_name;
  }

  /**
   * @param p_name the name to set
   */
  public void setName(String p_name)
  {
    m_name = p_name;
  }

  /**
   * @return the maxNumberOfPlayer
   */
  public int getMaxNumberOfPlayer()
  {
    return m_maxNumberOfPlayer;
  }

  /**
   * @param p_maxNumberOfPlayer the maxNumberOfPlayer to set
   */
  public void setMaxNumberOfPlayer(int p_maxNumberOfPlayer)
  {
    m_maxNumberOfPlayer = p_maxNumberOfPlayer;
  }



  /**
   * @return the isAsynchron
   */
  public boolean isAsynchron()
  {
    return getEbConfigGameTime().isAsynchron();
  }


  /**
   * @return the history
   * @WgtHidden
   */
  public boolean isHistory()
  {
    return m_history;
  }

  /**
   * @param p_history the history to set
   */
  public void setHistory(boolean p_history)
  {
    m_history = p_history;
  }


  /**
   * @return the currentTimeStep
   */
  public int getCurrentTimeStep()
  {
    return m_currentTimeStep;
  }

  /**
   * @param p_currentTimeStep the currentTimeStep to set
   */
  public void setCurrentTimeStep(int p_currentTimeStep)
  {
    m_currentTimeStep = p_currentTimeStep;
  }

  /**
   * @return the started
   */
  public boolean isStarted()
  {
    return m_started;
  }

  /**
   * @param p_started the started to set
   */
  public void setStarted(boolean p_started)
  {
    m_started = p_started;
    m_isOpen = isOpen();
  }


  /**
   * @return the description
   */
  public String getDescription()
  {
    return m_description;
  }

  /**
   * @param p_description the description to set
   */
  public void setDescription(String p_description)
  {
    m_description = p_description;
  }


  /**
   * @return the configGameTime
   */
  public ConfigGameTime getConfigGameTime()
  {
    return m_configGameTime;
  }

 
  /**
   * @return the configGameVariant
   */
  public ConfigGameVariant getConfigGameVariant()
  {
    return m_configGameVariant;
  }


  /**
   * @return the gameType
   */
  public GameType getGameType()
  {
    return m_gameType;
  }

  /**
   * @param p_gameType the gameType to set
   */
  public void setGameType(GameType p_gameType)
  {
    m_gameType = p_gameType;
  }

 
  /**
   * @return the planetType
   */
  public PlanetType getPlanetType()
  {
    return m_planetType;
  }

  /**
   * @param p_planetType the planetType to set
   */
  public void setPlanetType(PlanetType p_planetType)
  {
    m_planetType = p_planetType;
  }

  /**
   * @return the minimapUri
   */
  public String getMinimapUri()
  {
    return m_minimapUri;
  }

  /**
   * @param p_minimapUri the minimapUri to set
   */
  public void setMinimapUri(String p_minimapUri)
  {
    m_minimapUri = p_minimapUri;
  }

  /**
   * @return the minimapBlobKey
   */
  public String getMinimapBlobKey()
  {
    return m_minimapBlobKey;
  }

  /**
   * @param p_minimapBlobKey the minimapBlobKey to set
   */
  public void setMinimapBlobKey(String p_minimapBlobKey)
  {
    m_minimapBlobKey = p_minimapBlobKey;
  }

  /**
   * Don't forget to reset to null m_configGameTimeDefault if you change any value of this config
   * @return the configGameTime
   */
  public void setEbConfigGameTime(EbConfigGameTime p_config)
  {
    m_ebConfigGameTime = p_config;
  }

  /**
   * Don't forget to reset to null m_configGameTimeDefault if you change any value of this config
   * @return the configGameTime
   */
  public EbConfigGameTime getEbConfigGameTime()
  {
    // this patch is to handle old data game
    if(m_ebConfigGameTime == null && getConfigGameTime() != null)
    {
      setConfigGameTime( getConfigGameTime() );
    }
    return m_ebConfigGameTime;
  }


  /**
   * Don't forget to reset to null m_configGameVariantDefault if you change any value of this config
   * @return the configGameVariant
   */
  public void setEbConfigGameVariant(EbConfigGameVariant p_config)
  {
    m_ebConfigGameVariant = p_config;
  }

  /**
   * Don't forget to reset to null m_configGameVariantDefault if you change any value of this config
   * @return the configGameVariant
   */
  public EbConfigGameVariant getEbConfigGameVariant()
  {
    // this patch is to handle old data game
    if(m_ebConfigGameVariant == null && getConfigGameVariant() != null)
    {
      setConfigGameVariant( getConfigGameVariant() );
      assert m_ebConfigGameVariant != null;
      m_ebConfigGameVariant.multiplyConstructQty( getMaxNumberOfPlayer() );
    }
    return m_ebConfigGameVariant;
  }


  /**
   * @param p_configGameTime the configGameTime to set
   */
  public void setConfigGameTime(ConfigGameTime p_configGameTime)
  {
    m_configGameTime = p_configGameTime;
    setEbConfigGameTime( new EbConfigGameTime(m_configGameTime.getEbConfigGameTime()) );
  }

  /**
   * @param p_configGameVariant the configGameVariant to set
   */
  public void setConfigGameVariant(ConfigGameVariant p_configGameVariant)
  {
    m_configGameVariant = p_configGameVariant;
    setEbConfigGameVariant( new EbConfigGameVariant(m_configGameVariant.getEbConfigGameVariant()) );
  }

  public long getVersion()
  {
    return m_version;
  }

  /**
   * @param p_version the version to set
   */
  public void setVersion(long p_version)
  {
    m_version = p_version;
  }


  public void incVersion()
  {
    m_version++;
  }

  public void decVersion()
  {
    m_version--;
  }

  public boolean isVip()
  {
    return m_isVip;
  }

  public void setVip(boolean p_isVip)
  {
    m_isVip = p_isVip;
  }


  /**
   * @return the lastUpdate
   */
  public Date getLastUpdate()
  {
    return m_lastUpdate;
  }


}