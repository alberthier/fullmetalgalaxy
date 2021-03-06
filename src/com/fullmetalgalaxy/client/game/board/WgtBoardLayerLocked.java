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
 *  Copyright 2010 to 2015 Vincent Legendre
 *
 * *********************************************************************/
package com.fullmetalgalaxy.client.game.board;

import java.util.Collection;
import java.util.LinkedList;

import com.fullmetalgalaxy.client.game.GameEngine;
import com.fullmetalgalaxy.client.ressources.Icons;
import com.fullmetalgalaxy.model.EnuZoom;
import com.fullmetalgalaxy.model.LandType;
import com.fullmetalgalaxy.model.SharedMethods;
import com.fullmetalgalaxy.model.constant.FmpConstant;
import com.fullmetalgalaxy.model.persist.AnBoardPosition;
import com.fullmetalgalaxy.model.persist.EbRegistration;
import com.fullmetalgalaxy.model.persist.Game;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;

/**
 * @author Vincent Legendre
 *
 */
public class WgtBoardLayerLocked extends WgtBoardLayerBase
{
  /** a list of images widget that are currently used on board */
  private Collection<Image> m_usedImages = new LinkedList<Image>();
  private Collection<Image> m_unusedImages = new LinkedList<Image>();

  /**
   * 
   */
  public WgtBoardLayerLocked()
  {
  }




  /* (non-Javadoc)
   * @see com.fullmetalgalaxy.client.board.test.BoardLayerBase#onModelChange()
   */
  @Override
  public void onModelChange(boolean p_forceRedraw)
  {
    super.onModelChange( p_forceRedraw );
    redrawLock();
  }

  /* (non-Javadoc)
   * @see com.fullmetalgalaxy.client.board.test.BoardLayerBase#setZoom(com.fullmetalgalaxy.model.EnuZoom)
   */
  @Override
  public void setZoom(EnuZoom p_zoom)
  {
    super.setZoom( p_zoom );
    redrawLock();
  }

  private Image addImage()
  {
    AbstractImagePrototype imageprototype = AbstractImagePrototype.create( Icons.s_instance
        .strategy_padlock() );
    if( GameEngine.model().getZoomDisplayed().getValue() == EnuZoom.Medium )
    {
      imageprototype = AbstractImagePrototype.create( Icons.s_instance.tactic_padlock() );
    }
    Image image = null;
    if( !m_unusedImages.isEmpty() )
    {
      image = m_unusedImages.iterator().next();
      imageprototype.applyTo( image );
      image.setVisible( true );
      m_unusedImages.remove( image );
    }
    else
    {
      image = imageprototype.createImage();
      add( image );
      DOM.setStyleAttribute( image.getElement(), "zIndex", "1000" );
      image.addStyleName( "transparent50" );
    }
    m_usedImages.add( image );
    return image;
  }

  private void removeAllImages()
  {
    for( Image image : m_usedImages )
    {
      image.setVisible( false );
    }
    m_unusedImages.addAll( m_usedImages );
    m_usedImages.clear();
  }

  private void redrawLock()
  {
    // some game never have to display padlock...
    Game game = GameEngine.model().getGame();
    if( !game.isParallel()
        || game.getCurrentTimeStep() <= game.getEbConfigGameTime().getDeploymentTimeStep()
        || GameEngine.model().isTimeLineMode() )
    {
      // don't display padlock during deployment
      return;
    }

    // clear padlock icon on board
    removeAllImages();

    // then add padlock on board for other player
    EbRegistration myRegistration = GameEngine.model().getMyRegistration();
    for( EbRegistration registration : game.getSetRegistration() )
    {
      if( registration != myRegistration && registration.getTeam( game ).getEndTurnDate() != null
          && registration.getTeam( game ).getLockedPosition() != null )
      {
        if( registration.getTeam( game ).getEndTurnDate().getTime() < SharedMethods
            .currentTimeMillis() )
        {
          registration.getTeam( game ).setEndTurnDate( null );
          registration.getTeam( game ).setLockedPosition( null );
        }
        else
        {
          // display lock icon on board...
          for( AnBoardPosition position : GameEngine.coordinateSystem().drawHexagon( registration.getTeam( game )
              .getLockedPosition(), FmpConstant.parallelLockRadius ) )
          {
            if( game.getLand( position ) != LandType.None )
            {
              setWidgetHexPosition( addImage(), position );
            }
          }
          // then schedule hmi resfresh
          m_clockTimer
              .schedule( (int)(registration.getTeam( game ).getEndTurnDate().getTime() - SharedMethods
              .currentTimeMillis()) );
        }
      }
    }
  }

  @Override
  public void cropDisplay(int p_cropLeftHex, int p_cropTopHex, int p_cropRightHex,
      int p_cropBotomHex)
  {
    super.cropDisplay( p_cropLeftHex, p_cropTopHex, p_cropRightHex, p_cropBotomHex );
    redraw();
  }


  private Timer m_clockTimer = new Timer()
  {
    @Override
    public void run()
    {
      delayMillis = Integer.MAX_VALUE;
      redrawLock();
    }

    private int delayMillis = Integer.MAX_VALUE;

    @Override
    public void schedule(int p_delayMillis)
    {
      if( p_delayMillis > delayMillis )
        return;
      delayMillis = p_delayMillis;
      super.schedule( delayMillis );
    }

  };



}
