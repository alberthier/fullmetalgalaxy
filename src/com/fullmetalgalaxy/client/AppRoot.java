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
package com.fullmetalgalaxy.client;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.fullmetalgalaxy.client.event.EventPreviewListenerCollection;
import com.fullmetalgalaxy.client.event.SourcesPreviewEvents;
import com.fullmetalgalaxy.client.ressources.Icons;
import com.fullmetalgalaxy.model.RpcUtil;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author Vincent Legendre
 *
 */

public class AppRoot implements EntryPoint, WindowResizeListener, HistoryListener,
    SourcesPreviewEvents, NativePreviewHandler
{
  public static Logger logger = Logger.getLogger("AppRoot");
  private static AppRoot s_instance = null;
  
  /**
   * @return the first instance of AppMain
   */
  public static AppRoot instance()
  {
    return s_instance;
  }
  
  protected PopupPanel m_loadingPanel = new PopupPanel( false, true );
  protected int m_isLoading = 0;
  protected Map m_dialogMap = new HashMap();

  private HistoryState m_historyState = new HistoryState();
  private EventPreviewListenerCollection m_previewListenerCollection = new EventPreviewListenerCollection();
  private EventBus m_eventBus = new SimpleEventBus();
  
  
  /**
   * 
   */
  public AppRoot()
  {
    s_instance = this;
  }

  /* (non-Javadoc)
   * @see com.google.gwt.core.client.EntryPoint#onModuleLoad()
   */
  @Override
  public void onModuleLoad()
  {
    m_loadingPanel.setWidget( Icons.s_instance.loading().createImage() );
    m_loadingPanel.setVisible( true );
    m_loadingPanel.setStyleName( "gwt-DialogBox" );
    m_loadingPanel.addStyleName( "fmp-loading" );


    // Hook the window resize event, so that we can adjust the UI.
    // onWindowResized( Window.getClientWidth(), Window.getClientHeight() );
    Window.addWindowResizeListener( this );

    // Hook the preview event.
    // no other element should hook this event as only one can receive it.
    Event.addNativePreviewHandler( this );

    // Add history listener
    History.addHistoryListener( this );

    // If the application starts with no history token, start it off in the
    // 'baz' state.
    String initToken = History.getToken();
    m_historyState.fromString( initToken );
    if(initToken == null || initToken.isEmpty())
    {
      m_historyState = getDefaultHistoryState();
      initToken = m_historyState.toString();
    }
    
    // onHistoryChanged() is not called when the application first runs. Call
    // it now in order to reflect the initial state.
    onHistoryChanged( initToken );
    
  }

  public static EventBus getEventBus()
  {
    return instance().m_eventBus;
  }

  /* (non-Javadoc)
   * @see com.fullmetalgalaxy.client.SourcesPreviewEvents#addPreviewListener(com.google.gwt.user.client.EventPreview)
   */
  @Override
  public void addPreviewListener(NativePreviewHandler p_listener)
  {
    if( !m_previewListenerCollection.contains( p_listener ) )
    {
      m_previewListenerCollection.add( p_listener );
    }
  }

  /* (non-Javadoc)
   * @see com.fullmetalgalaxy.client.SourcesPreviewEvents#removePreviewListener(com.google.gwt.user.client.EventPreview)
   */
  @Override
  public void removePreviewListener(NativePreviewHandler p_listener)
  {
    m_previewListenerCollection.remove( p_listener );
  }



  /* (non-Javadoc)
   * @see com.google.gwt.user.client.Event.NativePreviewHandler#onPreviewNativeEvent(com.google.gwt.user.client.Event.NativePreviewEvent)
   */
  @Override
  public void onPreviewNativeEvent(NativePreviewEvent p_event)
  {
    m_previewListenerCollection.fireEventPreview( p_event );
  }


  /**
   * 
   * @return the current history state.
   */
  public HistoryState getHistoryState()
  {
    return m_historyState;
  }

  /**
   * Called when the browser window is resized.
   */
  @Override
  public void onWindowResized(int p_width, int p_height)
  {
    if( m_isLoading > 0 )
    {
      m_loadingPanel.center();
    }
    // m_dockPanel.setHeight( "" + (p_height - 20) + "px" );
  }



  /**
   * this method should return the default history token which contain the first MiniApp
   * which have to be displayed on module loading.
   * @return
   */
  public HistoryState getDefaultHistoryState()
  {
    if( RootPanel.get( "app_history" ) != null )
    {
      return new HistoryState( DOM.getElementAttribute(
          RootPanel.get( "app_history" ).getElement(), "content" ) );
    }
    return new HistoryState();
  }

  /**
   * @see com.google.gwt.user.client.HistoryListener#onHistoryChanged(java.lang.String)
   */
  @Override
  public void onHistoryChanged(String p_historyToken)
  {
    
  }

 
  public void startLoading()
  {
    if( m_isLoading < 0 )
    {
      m_isLoading = 0;
    }
    m_isLoading++;
    m_loadingPanel.show();
    m_loadingPanel.center();
  }

  public void stopLoading()
  {

    if( m_isLoading > 0 )
    {
      m_isLoading--;
    }
    if( m_isLoading == 0 )
    {
      m_loadingPanel.hide();
    }
  }

  public boolean isLoading()
  {
    return m_isLoading > 0;
  }



}
