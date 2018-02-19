/*
 * Created on May 10, 2013
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package com.biglybt.plugin.net.buddy.swt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.biglybt.ui.mdi.MdiCloseListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;

import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.AENetworkClassifier;
import com.biglybt.core.util.Base32;
import com.biglybt.core.util.Debug;
import com.biglybt.pif.ui.UIInstance;
import com.biglybt.pif.ui.UIManager;
import com.biglybt.pif.ui.UIManagerListener;
import com.biglybt.pif.ui.UIPluginViewToolBarListener;
import com.biglybt.pif.ui.tables.TableColumn;
import com.biglybt.pif.ui.tables.TableColumnCreationListener;
import com.biglybt.pif.ui.toolbar.UIToolBarItem;
import com.biglybt.pifimpl.local.PluginInitializer;
import com.biglybt.ui.swt.Messages;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.pifimpl.UISWTViewEventListenerHolder;
import com.biglybt.ui.swt.shells.MessageBoxShell;
import com.biglybt.ui.swt.views.table.TableViewSWT;
import com.biglybt.ui.swt.views.table.TableViewSWTMenuFillListener;
import com.biglybt.ui.swt.views.table.impl.TableViewFactory;

import com.biglybt.ui.UIFunctions;
import com.biglybt.ui.UIFunctionsManager;
import com.biglybt.ui.UserPrompterResultListener;
import com.biglybt.ui.common.ToolBarItem;
import com.biglybt.ui.common.table.TableColumnCore;
import com.biglybt.ui.common.table.TableRowCore;
import com.biglybt.ui.common.table.TableSelectionListener;
import com.biglybt.ui.common.table.TableViewFilterCheck;
import com.biglybt.ui.common.table.impl.TableColumnManager;
import com.biglybt.ui.common.updater.UIUpdatable;
import com.biglybt.ui.mdi.MdiEntry;
import com.biglybt.ui.mdi.MdiEntryCreationListener2;
import com.biglybt.ui.mdi.MultipleDocumentInterface;
import com.biglybt.ui.swt.UIFunctionsManagerSWT;
import com.biglybt.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.biglybt.ui.swt.skin.SWTSkinObject;
import com.biglybt.ui.swt.views.skin.InfoBarUtil;
import com.biglybt.ui.swt.views.skin.SkinView;
import com.biglybt.plugin.net.buddy.BuddyPluginBeta;
import com.biglybt.plugin.net.buddy.BuddyPluginUtils;
import com.biglybt.plugin.net.buddy.BuddyPluginBeta.*;
import com.biglybt.plugin.net.buddy.swt.columns.*;

/**
 * @author TuxPaper
 * @created May 10, 2013
 *
 */
public class SBC_ChatOverview
	extends SkinView
	implements 	UIUpdatable, UIPluginViewToolBarListener, TableViewFilterCheck<ChatInstance>,
				ChatManagerListener, TableViewSWTMenuFillListener, TableSelectionListener
{
	public static final int[] COLOR_MESSAGE_WITH_NICK = { 132, 16, 58 };

	private static final String TABLE_CHAT = "ChatsView";

	protected static final Object	MDI_KEY = new Object();

	public static void
	preInitialize()
	{
		UIManager ui_manager = PluginInitializer.getDefaultInterface().getUIManager();

		ui_manager.addUIListener(
				new UIManagerListener()
				{
					@Override
					public void
					UIAttached(
						UIInstance		instance )
					{
						final MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

						if ( mdi == null ){

							return;
						}

						mdi.registerEntry(
							"Chat_.*",
							new MdiEntryCreationListener2()
							{
								@Override
								public MdiEntry
								createMDiEntry(
									MultipleDocumentInterface mdi,
									String id,
									Object datasource,
									Map<?, ?> params)
								{
									ChatInstance chat = null;

									if ( datasource instanceof ChatInstance ){

										chat = (ChatInstance)datasource;

										try{
											chat = chat.getClone();

										}catch( Throwable e ){

											chat = null;

											Debug.out( e );
										}

									}else if ( id.length() > 7 ){

										BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();

										if ( beta != null ){

											try{
												String[] bits = id.substring( 5 ).split( ":" );

												String network 	= AENetworkClassifier.internalise( bits[0] );
												String key		= new String( Base32.decode( bits[1] ), "UTF-8" );

												chat = beta.getChat(network, key);

											}catch( Throwable e ){

												Debug.out( e );
											}
										}
									}

									if ( chat != null ){

										chat.setAutoNotify( true );

										return( createChatMdiEntry( chat ));
									}

									return( null );
								}
							});
					}

					@Override
					public void
					UIDetached(
						UIInstance instance)
					{
					}
				});
	}

	public static void
	openChat(
		String		network,
		String		key )
	{
		BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();

		if ( beta != null ){

			try{
				ChatInstance chat = beta.getChat(network, key);

				chat.setAutoNotify( true );

				MdiEntry mdi_entry = createChatMdiEntry( chat );

				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

				if ( mdi != null ){

					mdi.showEntry( mdi_entry );
				}
			}catch( Throwable e ){

				Debug.out( e );
			}
		}
	}

	private static MdiEntry
	createChatMdiEntry(
			final ChatInstance chat )
	{
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();

		if ( mdi == null ){

				// closing down

			return( null );
		}

		try{
			String key = "Chat_" + chat.getNetwork() + ":" + Base32.encode( chat.getKey().getBytes( "UTF-8" ));

			MdiEntry existing = mdi.getEntry( key );

			if ( existing != null ){

				chat.destroy();

				return( existing );
			}

			BuddyPluginBeta bp = BuddyPluginUtils.getBetaPlugin();

			TreeMap<ChatInstance,String>	name_map =
					new TreeMap<>(
						new Comparator<ChatInstance>()
						{
							@Override
							public int
							compare(
								ChatInstance o1,
								ChatInstance o2)
							{
								return( o1.getName().compareTo( o2.getName()));
							}
						});

			name_map.put( chat, key );

			List<ChatInstance> all_chats = bp.getChats();

			for ( ChatInstance c: all_chats ){

				try{
					String k = "Chat_" + c.getNetwork() + ":" + Base32.encode( c.getKey().getBytes( "UTF-8" ));

					if ( mdi.getEntry( k ) != null ){

						name_map.put( c, k );
					}
				}catch( Throwable e ){

				}
			}

			String	prev_id = null;

			for ( String this_id: name_map.values()){

				if ( this_id == key ){

					break;
				}

				prev_id = this_id;
			}

			if ( prev_id == null && name_map.size() > 1 ){

				Iterator<String>	it = name_map.values().iterator();

				it.next();

				prev_id = "~" + it.next();
			}

			MdiEntry entry =
				mdi.createEntryFromEventListener(
					MultipleDocumentInterface.SIDEBAR_SECTION_CHAT,
					new UISWTViewEventListenerHolder(
						key,
						ChatView.class,
						chat, null ),
					key, true, chat, prev_id);

			ChatMDIEntry entry_info = new ChatMDIEntry( chat, entry );

			chat.setUserData( MDI_KEY, entry_info );

			entry.addListener(new MdiCloseListener() {
				@Override
				public void mdiEntryClosed(MdiEntry entry, boolean userClosed) {
					chat.setUserData(MDI_KEY, null);
				}
			});

			return( entry );

		}catch( Throwable e ){

			Debug.out( e );

			return( null );
		}
	}


	TableViewSWT<ChatInstance> tv;

	private Text txtFilter;

	private Composite table_parent;

	private boolean columnsAdded = false;

	private boolean listener_added;

	@Override
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
	                                    Object datasource) {
		if ( tv == null || !tv.isVisible()){
			return( false );
		}
		if (item.getID().equals("remove")) {

			Object[] datasources = tv.getSelectedDataSources().toArray();

			if ( datasources.length > 0 ){

				for (Object object : datasources) {
					if (object instanceof ChatInstance) {
						ChatInstance chat = (ChatInstance) object;
						chat.remove();
					}
				}

				return true;
			}
		}

		return false;
	}

	@Override
	public void filterSet(String filter) {
	}

	@Override
	public void refreshToolBarItems(Map<String, Long> list) {
		if ( tv == null || !tv.isVisible()){
			return;
		}

		boolean canEnable = false;
		Object[] datasources = tv.getSelectedDataSources().toArray();

		if ( datasources.length > 0 ){

			for (Object object : datasources) {
				if (object instanceof ChatInstance ) {
					canEnable = true;
				}
			}
		}

		list.put("remove", canEnable ? UIToolBarItem.STATE_ENABLED : 0);
	}

	@Override
	public void updateUI() {
		if (tv != null) {
			tv.refreshTable(false);
		}
	}

	@Override
	public String getUpdateUIName() {
		return "ChatsView";
	}

	@Override
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		initColumns();

		new InfoBarUtil(skinObject, "chatsview.infobar", false,
				"chats.infobar", "chats.view.infobar") {
			@Override
			public boolean allowShow() {
				return true;
			}
		};

		return null;
	}

	protected void initColumns() {
		synchronized (SBC_ChatOverview.class) {

			if (columnsAdded) {

				return;
			}

			columnsAdded = true;
		}

		TableColumnManager tableManager = TableColumnManager.getInstance();

		tableManager.registerColumn(ChatInstance.class, ColumnChatName.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatName(column);
					}
				});

		tableManager.registerColumn(ChatInstance.class, ColumnChatMessageCount.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatMessageCount(column);
					}
				});

		tableManager.registerColumn(ChatInstance.class, ColumnChatUserCount.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatUserCount(column);
					}
				});

		tableManager.registerColumn(ChatInstance.class, ColumnChatFavorite.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatFavorite(column);
					}
				});

		tableManager.registerColumn(ChatInstance.class, ColumnChatMsgOutstanding.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatMsgOutstanding(column);
					}
				});


			// last

		tableManager.registerColumn(ChatInstance.class, ColumnChatStatus.COLUMN_ID,
				new TableColumnCreationListener() {
					@Override
					public void tableColumnCreated(TableColumn column) {
						new ColumnChatStatus(column);
					}
				});

		tableManager.setDefaultColumnNames(TABLE_CHAT,
			new String[] {

				ColumnChatName.COLUMN_ID,
				ColumnChatMessageCount.COLUMN_ID,
				ColumnChatUserCount.COLUMN_ID,
				ColumnChatFavorite.COLUMN_ID,
				ColumnChatMsgOutstanding.COLUMN_ID,

				ColumnChatStatus.COLUMN_ID,

			});

		tableManager.setDefaultSortColumnName(TABLE_CHAT, ColumnChatName.COLUMN_ID);
	}

	@Override
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {

		if (tv != null) {

			tv.delete();

			tv = null;
		}

		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});

		BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();

		if ( beta != null) {

			beta.removeListener( this );

			listener_added = false;
		}


		return super.skinObjectHidden(skinObject, params);
	}

	@Override
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);
		SWTSkinObject so_list = getSkinObject("chats-list");

		if (so_list != null) {
			initTable((Composite) so_list.getControl());
		} else {
			System.out.println("NO chats-list");
			return null;
		}

		if (tv == null) {
			return null;
		}

		BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();

		if ( beta != null) {

			if ( !listener_added ){

				listener_added = true;

				beta.addListener(this, true);
			}
		}

		return null;
	}

	@Override
	public Object
	skinObjectDestroyed(
		SWTSkinObject skinObject,
		Object params)
	{
		if ( listener_added ){

			listener_added = false;

			BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();

			if ( beta != null) {

				beta.removeListener( this );
			}
		}

		return super.skinObjectDestroyed(skinObject, params);
	}


	private void initTable(Composite control) {
		if ( tv == null ){

			tv = TableViewFactory.createTableViewSWT(ChatInstance.class, TABLE_CHAT, TABLE_CHAT,
					new TableColumnCore[0], ColumnChatName.COLUMN_ID, SWT.MULTI
							| SWT.FULL_SELECTION | SWT.VIRTUAL);
			if (txtFilter != null) {
				tv.enableFilterCheck(txtFilter, this);
			}
			tv.setRowDefaultHeightEM(1);

			table_parent = new Composite(control, SWT.BORDER);
			table_parent.setLayoutData(Utils.getFilledFormData());
			GridLayout layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
			table_parent.setLayout(layout);

			tv.addMenuFillListener( this );
			tv.addSelectionListener(this, false);

			tv.addKeyListener(
					new KeyAdapter(){
						@Override
						public void
						keyPressed(
							KeyEvent e )
						{
							if ( e.stateMask == 0 && e.keyCode == SWT.DEL ){
								
								Object[] datasources = tv.getSelectedDataSources().toArray();

								if ( datasources.length > 0 ){

									List<ChatInstance> chats = new ArrayList<>();
									
									String str = "";
									
									for (Object object : datasources){
										
										if  (object instanceof ChatInstance ){

											ChatInstance chat = (ChatInstance)object;
											
											chats.add( chat );
											
											if ( chats.size() == 1 ){
												
												str = chat.getDisplayName();
												
												if ( str == null ){
													
													str = chat.getName();
												}
											}else if ( chats.size() == 2 ){
												
												str += ", ...";
											}
										}
									}
									
									MessageBoxShell mb =
											new MessageBoxShell(
												MessageText.getString("message.confirm.delete.title"),
												MessageText.getString("message.confirm.delete.text",
														new String[] { str	}),
												new String[] {
													MessageText.getString("Button.yes"),
													MessageText.getString("Button.no")
												},
												1 );

										mb.open(new UserPrompterResultListener() {
											@Override
											public void prompterClosed(int result) {
												if (result == 0) {
												
													for ( ChatInstance chat: chats ){
														
														chat.remove();
													}
												}}});
								}
							}
						}
					});


			tv.initialize(table_parent);
		}

		control.layout(true);
	}

	@Override
	public void
	fillMenu(
		String 	sColumnName,
		Menu 	menu )
	{
		List<Object>	ds = tv.getSelectedDataSources();

		final List<ChatInstance>	chats = new ArrayList<>();

		for ( Object obj: ds ){

			if ( obj instanceof ChatInstance ){

				chats.add((ChatInstance)obj);
			}
		}

			// show in sidebar

		MenuItem itemSiS = new MenuItem(menu, SWT.PUSH);

		Messages.setLanguageText(itemSiS, Utils.isAZ2UI()?"label.show.in.tab":"label.show.in.sidebar");

		itemSiS.setEnabled(chats.size() > 0);

		itemSiS.addListener(SWT.Selection, new Listener() {
			@Override
			public void
			handleEvent(
				Event event )
			{
				MdiEntry	first_entry = null;

				for ( ChatInstance chat: chats ){

					try{
						MdiEntry entry = createChatMdiEntry( chat.getClone());

						if ( first_entry == null ){

							first_entry = entry;
						}
					}catch( Throwable e ){

						Debug.out( e );
					}
				}

				if ( first_entry != null ){

					MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

					if ( mdi != null ){

						mdi.showEntry(first_entry);
					}
				}
			}
		});



		MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);

		Messages.setLanguageText(itemRemove, "MySharesView.menu.remove");

		Utils.setMenuItemImage(itemRemove, "delete");

		itemRemove.setEnabled(chats.size() > 0);

		itemRemove.addListener(SWT.Selection, new Listener() {
			@Override
			public void
			handleEvent(
				Event e )
			{
				for ( ChatInstance chat: chats ){

					chat.remove();
				}
			}
		});

		new MenuItem( menu, SWT.SEPARATOR );
	}

	@Override
	public void
	addThisColumnSubMenu(
		String 	sColumnName,
		Menu	menuThisColumn )
	{

	}

	@Override
	public void
	selected(
		TableRowCore[] row )
	{
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	if (uiFunctions != null) {
	  		uiFunctions.refreshIconBar();
	  	}
	}

	@Override
	public void
	deselected(
		TableRowCore[] rows )
	{
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	if (uiFunctions != null) {
	  		uiFunctions.refreshIconBar();
	  	}
	}

	@Override
	public void
	focusChanged(
		TableRowCore focus )
	{
	  	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	if (uiFunctions != null) {
	  		uiFunctions.refreshIconBar();
	  	}
	}

	@Override
	public void
	defaultSelected(
		TableRowCore[] 	rows,
		int 			stateMask )
	{
		if ( rows.length == 1 ){

			Object obj = rows[0].getDataSource();

			if ( obj instanceof ChatInstance ){

				ChatInstance chat = (ChatInstance)obj;

				BuddyPluginBeta beta = BuddyPluginUtils.getBetaPlugin();

				if ( beta != null) {

					try{
						beta.showChat( chat.getClone());

					}catch( Throwable e ){

						Debug.out( e );
					}
				}
			}
		}
	}

	@Override
	public void chatAdded(ChatInstance chat) {
		if ( !chat.isInvisible()){
			tv.addDataSource(chat);
		}
	}

	public void chatChanged(ChatInstance chat) {
		if (tv == null || tv.isDisposed()) {
			return;
		}
		TableRowCore row = tv.getRow(chat);
		if (row != null) {
			row.invalidate(true);
		}
	}

	@Override
	public void chatRemoved(ChatInstance chat) {
		tv.removeDataSource(chat);
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	  	if (uiFunctions != null) {
	  		uiFunctions.refreshIconBar();
	  	}
	}

	@Override
	public void
	mouseEnter(
		TableRowCore row )
	{
	}

	@Override
	public void
	mouseExit(
		TableRowCore row)
	{
	}

	@Override
	public boolean filterCheck(ChatInstance ds, String filter, boolean regex) {
		return false;
	}
}
