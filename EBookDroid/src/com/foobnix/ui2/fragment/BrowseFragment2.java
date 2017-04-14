package com.foobnix.ui2.fragment;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.ui2.adapter.DefaultListeners;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fast.FastScrollRecyclerView;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.util.Pair;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupMenu;
import android.widget.TextView;

public class BrowseFragment2 extends UIFragment<FileMeta> {
    public static final String EXTRA_INIT_PATH = "EXTRA_PATH";
    public static final String EXTRA_TYPE = "EXTRA_TYPE";
    public static final String EXTRA_TEXT = "EXTRA_TEXT";
    public static int TYPE_DEFAULT = 0;
    public static int TYPE_SELECT_FOLDER = 1;
    public static int TYPE_SELECT_FILE = 2;
    public static int TYPE_CREATE_FILE = 3;

    FileMetaAdapter searchAdapter;
    private FastScrollRecyclerView recyclerView;

    private LinearLayout paths;
    HorizontalScrollView scroller;
    private TextView stub;
    private ImageView onListGrid;
    private EditText editPath;
    private View pathContainer;

    private int fragmentType = TYPE_DEFAULT;
    private String fragmentText = "";

    public BrowseFragment2() {
        super();
    }

    public static BrowseFragment2 newInstance(Bundle bundle) {
        BrowseFragment2 br = new BrowseFragment2();
        br.setArguments(bundle);
        return br;
    }

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return new Pair<Integer, Integer>(R.string.folders, R.drawable.glyphicons_145_folder_open);
    }

    private ResultResponse<String> onPositiveAction;
    private ResultResponse<String> onCloseAction;

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(pathContainer, TintUtil.color);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse2, container, false);

        Bundle arguments = getArguments();

        pathContainer = view.findViewById(R.id.pathContainer);
        View onCloseActionPaner = view.findViewById(R.id.onCloseActionPaner);
        View onClose = view.findViewById(R.id.onClose);

        TextView onAction = (TextView) view.findViewById(R.id.onAction);
        editPath = (EditText) view.findViewById(R.id.editPath);

        fragmentType = TYPE_DEFAULT;
        if (arguments != null) {
            fragmentType = arguments.getInt(EXTRA_TYPE, TYPE_DEFAULT);
            fragmentText = arguments.getString(EXTRA_TEXT);
            editPath.setText(fragmentText);
        }

        onClose.setOnClickListener(onCloseButtonActoin);
        onAction.setOnClickListener(onSelectAction);

        if (TYPE_DEFAULT == fragmentType) {
            editPath.setVisibility(View.GONE);
            onCloseActionPaner.setVisibility(View.GONE);
        }
        if (TYPE_SELECT_FOLDER == fragmentType) {
            editPath.setVisibility(View.VISIBLE);
            editPath.setEnabled(false);
            onCloseActionPaner.setVisibility(View.VISIBLE);
        }
        if (TYPE_SELECT_FILE == fragmentType) {
            editPath.setVisibility(View.VISIBLE);
            editPath.setEnabled(false);
            onCloseActionPaner.setVisibility(View.VISIBLE);
        }
        if (TYPE_CREATE_FILE == fragmentType) {
            editPath.setVisibility(View.VISIBLE);
            editPath.setEnabled(true);
            onCloseActionPaner.setVisibility(View.VISIBLE);
        }

        View onBack = view.findViewById(R.id.onBack);
        recyclerView = (FastScrollRecyclerView) view.findViewById(R.id.recyclerView);

        paths = (LinearLayout) view.findViewById(R.id.paths);
        scroller = (HorizontalScrollView) view.findViewById(R.id.scroller);
        final View onHome = view.findViewById(R.id.onHome);
        onListGrid = (ImageView) view.findViewById(R.id.onListGrid);
        onListGrid.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMenu(onListGrid);
            }
        });

        searchAdapter = new FileMetaAdapter();
        bindAdapter(searchAdapter);
        bindAuthorsSeriesAdapter(searchAdapter);

        onGridList();

        onHome.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                List<String> externalStorageDirectories = ExtUtils.getExternalStorageDirectories(getActivity());
                if (externalStorageDirectories != null && !externalStorageDirectories.isEmpty()) {
                    externalStorageDirectories.add(0, Environment.getExternalStorageDirectory().getPath());
                    PopupMenu menu = new PopupMenu(getActivity(), onHome);
                    for (final String info : externalStorageDirectories) {
                        menu.getMenu().add(new File(info).getName()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                setDirPath(info);
                                return false;
                            }
                        });
                    }
                    menu.show();
                } else {
                    File downloads = Environment.getExternalStorageDirectory();
                    setDirPath(downloads.getPath());
                }

            }
        });
        onBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackAction();
            }
        });

        searchAdapter.setOnItemClickListener(new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(FileMeta result) {
                if (result.getCusType() != null && result.getCusType() == FileMetaAdapter.DISPLAY_TYPE_DIRECTORY) {
                    setDirPath(result.getPath());
                    if (fragmentType == TYPE_SELECT_FOLDER) {
                        editPath.setText(fragmentText);
                    }
                } else {
                    if (fragmentType == TYPE_DEFAULT) {
                        DefaultListeners.getOnItemClickListener(getActivity()).onResultRecive(result);
                    } else if (fragmentType == TYPE_SELECT_FILE) {
                        editPath.setText(ExtUtils.getFileName(result.getPath()));
                    }

                }
                return false;
            }
        });

        searchAdapter.setOnItemLongClickListener(new ResultResponse<FileMeta>() {
            @Override
            public boolean onResultRecive(FileMeta result) {
                if (result.getCusType() != null && result.getCusType() == FileMetaAdapter.DISPLAY_TYPE_DIRECTORY) {
                    // setDirPath(result.getPath());
                } else {
                    DefaultListeners.getOnItemLongClickListener(getActivity(), searchAdapter).onResultRecive(result);
                }
                return false;
            }
        });

        populate();
        onTintChanged();

        return view;
    }

    OnClickListener onCloseButtonActoin = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (onCloseAction != null) {
                onCloseAction.onResultRecive("");
            }
        }
    };

    OnClickListener onSelectAction = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (fragmentType == TYPE_SELECT_FOLDER) {
                onPositiveAction.onResultRecive(AppState.get().dirLastPath);
            } else if (fragmentType == TYPE_SELECT_FILE) {
                onPositiveAction.onResultRecive(AppState.get().dirLastPath + "/" + editPath.getText());
            } else if (fragmentType == TYPE_CREATE_FILE) {
                onPositiveAction.onResultRecive(AppState.get().dirLastPath + "/" + editPath.getText());
            }

        }
    };

    public String getInitPath() {
        try {
            String pathArgument = getArguments() != null ? getArguments().getString(EXTRA_INIT_PATH, null) : "";
            if (TxtUtils.isNotEmpty(pathArgument)) {
                return pathArgument;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        String path = AppState.get().dirLastPath == null ? Environment.getExternalStorageDirectory().getPath() : AppState.get().dirLastPath;
        if (new File(path).isDirectory()) {
            return path;
        }
        return Environment.getExternalStorageDirectory().getPath();
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        return SearchCore.getFilesAndDirs(getInitPath());
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        setDirPath(getInitPath(), items);
    }

    public boolean onBackAction() {
        File file = new File(AppState.get().dirLastPath);
        if (file.getParent() != null) {
            setDirPath(file.getParent());
            return true;
        }
        return false;
    }

    public void setDirPath(String path) {
        setDirPath(path, null);
    }

    public void setDirPath(String path, List<FileMeta> items) {
        if (searchAdapter == null) {
            return;
        }
        AppState.get().dirLastPath = path;

        searchAdapter.clearItems();
        if (items == null) {
            items = SearchCore.getFilesAndDirs(path);
        }
        searchAdapter.getItemsList().addAll(items);
        recyclerView.setAdapter(searchAdapter);

        paths.removeAllViews();

        final String[] split = path.split("/");

        if (split == null || split.length == 0) {
            TextView slash = new TextView(getActivity());
            slash.setText(" / ");
            slash.setTextColor(getResources().getColor(R.color.white));
            paths.addView(slash);
        } else {

            for (int i = 0; i < split.length; i++) {
                final int index = i;
                String part = split[i];
                if (TxtUtils.isEmpty(part)) {
                    continue;
                }
                TextView slash = new TextView(getActivity());
                slash.setText(" / ");
                slash.setTextColor(getResources().getColor(R.color.white));

                TextView item = new TextView(getActivity());
                item.setText(part);
                item.setGravity(Gravity.CENTER);
                item.setTextColor(getResources().getColor(R.color.white));
                item.setSingleLine();
                TypedValue outValue = new TypedValue();
                getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                item.setBackgroundResource(outValue.resourceId);

                if (i == split.length - 1) {
                    item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                } else {
                    item.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            StringBuilder builder = new StringBuilder();
                            for (int j = 0; j <= index; j++) {
                                builder.append("/");
                                builder.append(split[j]);
                            }
                            String itemPath = builder.toString();
                            setDirPath(itemPath);
                        }
                    });
                }

                paths.addView(slash);
                paths.addView(item, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            }

            TextView stub = new TextView(getActivity());
            stub.setText("    ");
            paths.addView(stub);

        }
        scroller.postDelayed(new Runnable() {

            @Override
            public void run() {
                scroller.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, 100);

    }

    public void onGridList() {
        if (searchAdapter == null) {
            return;
        }
        PopupHelper.updateGridOrListIcon(onListGrid, AppState.get().broseMode);

        if (AppState.get().broseMode == AppState.MODE_LIST) {
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST);
            recyclerView.setAdapter(searchAdapter);

        }

        if (AppState.get().broseMode == AppState.MODE_COVERS) {
            int num = Dips.screenWidthDP() / AppState.get().coverBigSize;
            RecyclerView.LayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            recyclerView.setLayoutManager(mGridManager);

            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_COVERS);
            recyclerView.setAdapter(searchAdapter);
        }

        if (AppState.get().broseMode == AppState.MODE_GRID) {
            int num = Dips.screenWidthDP() / AppState.get().coverBigSize;
            RecyclerView.LayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            recyclerView.setLayoutManager(mGridManager);

            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_GRID);
            recyclerView.setAdapter(searchAdapter);
        }

        recyclerView.myConfiguration();
    }

    private void popupMenu(final ImageView onGridList) {
        PopupMenu p = new PopupMenu(getActivity(), onGridList);
        List<Integer> names = Arrays.asList(R.string.list, R.string.grid, R.string.cover);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_156_show_big_thumbnails, R.drawable.glyphicons_157_show_thumbnails);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.getInstance().broseMode = actions.get(index);
                    onGridList.setImageResource(icons.get(index));
                    onGridList();
                    return false;
                }
            });
        }

        p.show();

        PopupHelper.initIcons(p, TintUtil.color);
    }

    @Override
    public boolean isBackPressed() {
        return onBackAction();
    }

    @Override
    public void notifyFragment() {
        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void resetFragment() {
        onGridList();
    }

    public void setOnPositiveAction(ResultResponse<String> onPositiveAction) {
        this.onPositiveAction = onPositiveAction;
    }

    public void setOnCloseAction(ResultResponse<String> onCloseAction) {
        this.onCloseAction = onCloseAction;
    }

}