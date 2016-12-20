// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.JBUI;
import com.microsoft.alm.plugin.external.commands.LockCommand;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public class LockItemsTableModel extends AbstractTableModel {

    public interface Listener extends EventListener {
        void selectionChanged();
    }

    enum Column {
        //        Selection("", 5) {
//            public Boolean getValue(final ItemInfo item) {
//                return item.getSelectionStatus();
//            }
//        },
        Item(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_LOCK_DIALOG_ITEM_COLUMN), 550) {
            public String getValue(final ItemInfo item) {
                return item.getServerItem();
            }
        },
        Lock(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_LOCK_DIALOG_LOCK_COLUMN), 110) {
            public String getValue(final ItemInfo item) {
                final LockCommand.LockLevel level = LockCommand.LockLevel.fromString(item.getLock());
                switch (level) {
                    case CHECKIN:
                        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_LOCK_DIALOG_LOCK_LEVEL_CHECKIN);
                    case CHECKOUT:
                        return TfPluginBundle.message(TfPluginBundle.KEY_TFVC_LOCK_DIALOG_LOCK_LEVEL_CHECKOUT);
                }
                // "None" is just the empty string
                return StringUtils.EMPTY;
            }
        },
        LockOwner(TfPluginBundle.message(TfPluginBundle.KEY_TFVC_LOCK_DIALOG_LOCKED_BY_COLUMN), 130) {
            public String getValue(final ItemInfo item) {
                return item.getLockOwner();
            }
        };

        private final String name;
        private final int width;

        Column(final String name, final int width) {
            this.name = name;
            this.width = JBUI.scale(width);
        }

        public String getName() {
            return name;
        }

        public int getWidth() {
            return width;
        }

        @Nullable
        public abstract Object getValue(final ItemInfo item);
    }

    private final List<ItemInfo> items;
    private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

    public LockItemsTableModel(final @NotNull List<ItemInfo> items) {
        this.items = items;
        //Collections.sort(items, ItemInfo.LOCK_ITEM_PARENT_FIRST);
    }

    public int getRowCount() {
        return items.size();
    }

    public int getColumnCount() {
        return Column.values().length;
    }

    public String getColumnName(final int column) {
        return Column.values()[column].getName();
    }

    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        //return Column.values()[columnIndex] == Column.Selection && items.get(rowIndex).getSelectionStatus() != null;
        return false;
    }

    @Nullable
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        return Column.values()[columnIndex].getValue(items.get(rowIndex));
    }

//    @Override
//    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
//        if (Column.values()[columnIndex] == Column.Selection) {
//            items.get(rowIndex).setSelectionStatus((Boolean) aValue);
//            myEventDispatcher.getMulticaster().selectionChanged();
//        }
//    }

    public List<ItemInfo> getSelectedItems() {
        final List<ItemInfo> result = new ArrayList<ItemInfo>();
        for (ItemInfo item : items) {
            //if (item.getSelectionStatus() == Boolean.TRUE) {
            result.add(item);
            //}
        }
        return result;
    }

    public void addListener(final Listener listener) {
        myEventDispatcher.addListener(listener);
    }

    public void removeListener(final Listener listener) {
        myEventDispatcher.removeListener(listener);
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
//        if (columnIndex == Column.Selection.ordinal()) {
//            return Boolean.class;
//        } else if (columnIndex == Column.Item.ordinal()) {
//            return ExtendedItem.class;
//        } else {
        return super.getColumnClass(columnIndex);
        //}
    }
}
