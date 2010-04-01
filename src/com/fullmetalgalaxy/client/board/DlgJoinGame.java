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
 *  Copyright 2010 Vincent Legendre
 *
 * *********************************************************************/
/**
 * 
 */
package com.fullmetalgalaxy.client.board;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fullmetalgalaxy.client.ModelFmpMain;
import com.fullmetalgalaxy.client.ressources.BoardIcons;
import com.fullmetalgalaxy.client.ressources.Messages;
import com.fullmetalgalaxy.model.EnuColor;
import com.fullmetalgalaxy.model.persist.gamelog.EbGameJoin;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Kroc
 *
 */
public class DlgJoinGame extends DialogBox implements ClickListener
{
  // UI
  private Map<Image, Integer> m_icons = new HashMap<Image, Integer>();
  private Button m_btnCancel = new Button( "Cancel" );
  private Panel m_panel = new FlowPanel();


  /**
   * 
   */
  public DlgJoinGame()
  {
    // auto hide / modal
    super( false, true );

    // Set the dialog box's caption.
    setText( "Choisissez votre couleur" );

    m_btnCancel.addClickListener( this );

    // configure color selector
    Set<EnuColor> freeColors = null;
    if( ModelFmpMain.model().getGame().getSetRegistration().size() >= ModelFmpMain.model()
        .getGame().getMaxNumberOfPlayer() )
    {
      freeColors = ModelFmpMain.model().getGame().getFreeRegistrationColors();
    }
    else
    {
      freeColors = ModelFmpMain.model().getGame().getFreePlayersColors();
    }
    for( EnuColor color : freeColors )
    {
      Image image = new Image();
      BoardIcons.icon64( color.getValue() ).applyTo( image );
      image.setTitle( Messages.getColorString( color.getValue() ) );
      image.addStyleName( "fmp-button" );
      image.addClickListener( this );
      m_icons.put( image, color.getValue() );
      m_panel.add( image );
    }

    m_panel.add( m_btnCancel );

    setWidget( m_panel );
  }


  /* (non-Javadoc)
   * @see com.google.gwt.user.client.ui.ClickListener#onClick(com.google.gwt.user.client.ui.Widget)
   */
  public void onClick(Widget p_sender)
  {
    if( p_sender == m_btnCancel )
    {
      this.hide();
      return;
    }

    int color = m_icons.get( p_sender );

    EbGameJoin action = new EbGameJoin();
    action.setGame( ModelFmpMain.model().getGame() );
    action.setAccountId( ModelFmpMain.model().getMyAccountId() );
    action.setColor( color );
    ModelFmpMain.model().runSingleAction( action );

    this.hide();
  }



  /* (non-Javadoc)
   * @see com.google.gwt.user.client.ui.PopupPanel#show()
   */
  public void show()
  {
    super.show();
  }
}
