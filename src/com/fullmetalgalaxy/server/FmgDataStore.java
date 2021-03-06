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
package com.fullmetalgalaxy.server;

import java.util.ArrayList;
import java.util.List;

import com.fullmetalgalaxy.model.persist.CompanyStatistics;
import com.fullmetalgalaxy.model.persist.EbGameData;
import com.fullmetalgalaxy.model.persist.EbGameLog;
import com.fullmetalgalaxy.model.persist.EbGamePreview;
import com.fullmetalgalaxy.model.persist.Game;
import com.fullmetalgalaxy.model.persist.PlayerGameStatistics;
import com.fullmetalgalaxy.model.persist.gamelog.AnEvent;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;

/**
 * Specific DAO for FMG
 * 
 * @author vlegendr
 *
 */
public class FmgDataStore extends DataStore
{
  static
  {
    ObjectifyService.register( EbAccount.class );
    ObjectifyService.register( EbGameData.class );
    ObjectifyService.register( EbGamePreview.class );
    ObjectifyService.register( EbGameLog.class );
    ObjectifyService.register( CompanyStatistics.class );
    ObjectifyService.register( PlayerGameStatistics.class );
  }

  /**
   * a static read only DAO
   */
  private final static FmgDataStore s_dao = new FmgDataStore( true );

  /**
   * This method return a static instance of a read only data store.
   * You can't use it to put and commit data.
   * @return
   */
  public static FmgDataStore dao()
  {
    return s_dao;
  }
  
  public static String getPseudoFromJid(String p_jid)
  {
    if( p_jid == null )
    {
      return "???";
    }
    String jid = p_jid.split("/")[0];
    Query<EbAccount> query = dao().query( EbAccount.class ).filter( "m_jabberId", jid );
    QueryResultIterator<EbAccount> it = query.iterator();
    if( !it.hasNext() )
    {
      // we could remove the end of jid @...
      // but in this case someone can use a jid in the form of <existingPseudo>@anydomain
      // to fool players !
      return jid;
    }
    return it.next().getPseudo();
  }
  
  /**
   * 
   * @param p_pseudo
   * @return true if p_pseudo exist as a login or pseudo in data base.
   */
  public static boolean isPseudoExist( String p_pseudo )
  {
    String pseudo = ServerUtil.compactTag( p_pseudo );
    Query<EbAccount> query = dao().query( EbAccount.class ).filter( "m_compactPseudo", pseudo );
    if( !query.fetchKeys().iterator().hasNext() )
    {
      query = dao().query( EbAccount.class ).filter( "m_login", p_pseudo );
      if( !query.fetchKeys().iterator().hasNext() )
      {
        return false;
      }
    }
    return true;
  }
  
  
  public static boolean updatePseudo(long p_accountId, String p_newPseudo)
  {
    if( !EbAccount.isValidPseudo( p_newPseudo ) )
    {
      return false;
    }
    FmgDataStore ds = new FmgDataStore( false );
    EbAccount account = ds.find( EbAccount.class, p_accountId );
    String oldPseudo = account.getPseudo();
    if( !ServerUtil.compactTag( oldPseudo ).equals( ServerUtil.compactTag( p_newPseudo ) )
        && isPseudoExist( p_newPseudo ) )
    {
      ds.rollback();
      return false;
    }
    account.setPseudo( p_newPseudo );
    ds.put( account );
    ds.commit();

    // TODO change all pseudo in all games...

    return true;
  }



  /**
   * @param p_isReadOnly
   */
  public FmgDataStore(boolean p_isReadOnly)
  {
    super( p_isReadOnly );
  }


  public EbGameLog getGameLog(Long p_id)
  {
    Key<EbGamePreview> keyPreview = new Key<EbGamePreview>( EbGamePreview.class, p_id );
    Key<EbGameLog> keyLog = new Key<EbGameLog>( keyPreview, EbGameLog.class, p_id );
    EbGameLog gameLog = find( keyLog );
    return gameLog;
  }

  /**
   * 
   * @param p_id
   * @return null if game not found
   */
  public Game getGame(Long p_id)
  {
    EbGamePreview preview = find( EbGamePreview.class, p_id );
    return getGame( preview );
  }

  public Game getGame( EbGamePreview p_preview )
  {
    if( p_preview == null )
    {
      return null;
    }
    Key<EbGamePreview> keyPreview = new Key<EbGamePreview>(EbGamePreview.class, p_preview.getId());
    Key<EbGameData> keyData = new Key<EbGameData>(keyPreview, EbGameData.class, p_preview.getId());
    EbGameData data = find( keyData );
    if( data == null )
    {
      return null;
    }
    return new Game( p_preview, data );
  }
  
  public Game getGame( Key<EbGamePreview> p_keyPreview )
  {
    if( p_keyPreview == null )
    {
      return null;
    }
    EbGamePreview preview = find(p_keyPreview); 
    if( preview == null )
    {
      return null;
    }
    Key<EbGameData> keyData = new Key<EbGameData>(p_keyPreview, EbGameData.class, preview.getId());
    EbGameData data = find( keyData );
    if( data == null )
    {
      return null;
    }
    return new Game( preview, data );
  }
  


  /**
   * put a game and his associated game preview
   * @param p_game
   * @return null if fail
   */
  public Long put(Game p_game)
  {
    if( isReadOnly() )
    {
      return null;
    }
    // to update is open flag
    p_game.updateOpenPauseStatus();

    Key<EbGamePreview> keyPreview = put( p_game.getPreview() );

    // should we split event log into several entity ?
    int maxEvents = EbGameLog.MAX_EVENTS_PER_BLOB;
    if( p_game.getAdditionalEventCount() > 0
        && maxEvents > EbGameLog.MAX_EVENTS_PER_PLAYER * p_game.getCurrentNumberOfRegiteredPlayer() )
    {
      maxEvents = EbGameLog.MAX_EVENTS_PER_PLAYER * p_game.getCurrentNumberOfRegiteredPlayer();
    }
    if( p_game.getLogs().size() > maxEvents )
    {
      // read last additional event log
      EbGameLog gameLog = null;
      int lastGameLogCount = 0;
      long lastGameLogId = 0;
      if( !p_game.getAdditionalGameLog().isEmpty() )
      {
        lastGameLogId = p_game.getAdditionalGameLog()
            .get( p_game.getAdditionalGameLog().size() - 1 );
        Key<EbGameLog> keyGameLog = new Key<EbGameLog>( keyPreview, EbGameLog.class, lastGameLogId );
        gameLog = find( keyGameLog );
      }
      if( gameLog == null )
      {
        lastGameLogId = 0;
        gameLog = new EbGameLog();
        gameLog.setIndex( 1 );
      }
      lastGameLogCount = gameLog.getLog().size();
      
      // split events log in two or three parts
      int minEvents = EbGameLog.MIN_EVENTS_PER_PLAYER * p_game.getCurrentNumberOfRegiteredPlayer();
      int sizePart1 =  p_game.getLogs().size() - minEvents;
      int sizePart2 = 0;
      if( sizePart1 > EbGameLog.MAX_EVENTS_PER_BLOB - lastGameLogCount )
      {
        sizePart1 = EbGameLog.MAX_EVENTS_PER_BLOB - lastGameLogCount;
        sizePart2 = p_game.getLogs().size() - minEvents - sizePart1;
      }
      List<AnEvent> logPart1 = p_game.getLogs().subList( 0, sizePart1 );
      List<AnEvent> logPart2 = p_game.getLogs().subList( sizePart1, sizePart1 + sizePart2 );
      List<AnEvent> logPart3 = p_game.getLogs().subList( sizePart1 + sizePart2, p_game.getLogs().size() );

      // store part 1
      gameLog.getLog().addAll( logPart1 );
      gameLog.setKeyPreview( keyPreview );
      put( gameLog );
      if( lastGameLogId == 0 )
      {
        p_game.getAdditionalGameLog().add( gameLog.getId() );
      }
      p_game.setAdditionalEventCount( p_game.getAdditionalEventCount() + logPart1.size() );

      // store part 2
      if( !logPart2.isEmpty() )
      {
        EbGameLog gameLog2 = new EbGameLog();
        gameLog2.setIndex( gameLog.getIndex() + 1 );
        gameLog2.getLog().addAll( logPart2 );
        gameLog2.setKeyPreview( keyPreview );
        put( gameLog2 );
        p_game.getAdditionalGameLog().add( gameLog2.getId() );
        p_game.setAdditionalEventCount( p_game.getAdditionalEventCount() + logPart2.size() );
      }

      // store part 3
      // can't use p_game.setLogs( logPart3 ); as we get:
      // NotSerializableException: java.util.ArrayList$SubList
      p_game.setLogs( new ArrayList<AnEvent>() );
      p_game.getLogs().addAll( logPart3 );

    }

    p_game.getData().setId( keyPreview.getId() );
    p_game.getData().setKeyPreview( keyPreview );
    put( p_game.getData() );
    return keyPreview.getId();
  }
  

  /**
   * delete both preview and data entity
   */
  protected void deleteGame(EbGamePreview p_gamePreview)
  {
    if( p_gamePreview != null )
    {
      Long id = p_gamePreview.getId();
      Key<EbGamePreview> keyPreview = new Key<EbGamePreview>(EbGamePreview.class, id );

      // delete all additional logs
      Iterable<Key<EbGameLog>> logs = FmgDataStore.dao().query( EbGameLog.class )
          .ancestor( keyPreview ).fetchKeys();
      super.delete( logs );
      Key<EbGameData> keyData = new Key<EbGameData>(keyPreview, EbGameData.class, id );
      super.delete( keyPreview, keyData );
    }
  }

  /* (non-Javadoc)
   * @see com.fullmetalgalaxy.server.DataStore#delete(java.lang.Object[])
   */
  @Override
  public void delete(Object p_object)
  {
    if( p_object instanceof EbGamePreview )
    {
      deleteGame( (EbGamePreview)p_object );
    }
    else if( p_object instanceof Game )
    {
      deleteGame( ((Game)p_object).getPreview() );
    }
    else
    {
      super.delete( p_object );
    }
  }

  /* (non-Javadoc)
   * @see com.fullmetalgalaxy.server.DataStore#delete(java.lang.Class, long)
   */
  @Override
  public <T> void delete(Class<T> p_arg0, long p_id)
  {
    if( p_arg0 == EbGamePreview.class || p_arg0 == Game.class )
    {
      EbGamePreview gamePreview = get( EbGamePreview.class, p_id );
      deleteGame( gamePreview );
    }
    else
    {
      super.delete( p_arg0, p_id );
    }
  }


  /* (non-Javadoc)
   * @see com.fullmetalgalaxy.server.DataStore#get(java.lang.Class, long)
   */
  @Override
  public <T> T get(Class<? extends T> p_arg0, long p_arg1) throws NotFoundException
  {
    if( p_arg0 == Game.class )
    {
      return p_arg0.cast( getGame( p_arg1 ) );
    }
    else
    {
      return super.get( p_arg0, p_arg1 );
    }
  }



}
