package com.alphawallet.app.ui;

import static com.alphawallet.app.C.Key.WALLET;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.adapter.TabPagerAdapter;
import com.alphawallet.app.ui.widget.entity.ScrollControlViewPager;
import com.alphawallet.app.util.TabUtils;
import com.alphawallet.app.viewmodel.NFTViewModel;
import com.alphawallet.app.viewmodel.NFTViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.google.android.material.tabs.TabLayout;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class NFTActivity extends BaseActivity implements StandardFunctionInterface {
    @Inject
    NFTViewModelFactory viewModelFactory;
    NFTViewModel viewModel;

    private Menu menu;
    private Wallet wallet;
    private Token token;
    private FunctionButtonBar functionBar;
    private boolean isGridView = true;

    private MenuItem sendMultipleTokensMenuItem;
    private MenuItem switchToGridViewMenuItem;
    private MenuItem switchToListViewMenuItem;

    private NFTAssetsFragment assetsFragment;

    private ActivityResultLauncher<Intent> handleTransactionSuccess = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) return;
                String transactionHash = result.getData().getStringExtra(C.EXTRA_TXHASH);
                //process hash
                if (!TextUtils.isEmpty(transactionHash))
                {
                    Intent intent = new Intent();
                    intent.putExtra(C.EXTRA_TXHASH, transactionHash);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc1155);
        toolbar();
        findViewById(R.id.user_address_blockie).setVisibility(View.GONE);
        initViewModel();
        getIntentData();
        setTitle(token.tokenInfo.name);
        setupViewPager();
    }

    public void storeAsset(BigInteger tokenId, NFTAsset asset)
    {
        viewModel.getTokensService().storeAsset(token, tokenId, asset);
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(NFTViewModel.class);
    }

    private void getIntentData()
    {
        wallet = getIntent().getParcelableExtra(WALLET);
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
    }

    private void setupViewPager()
    {
        NFTInfoFragment infoFragment = new NFTInfoFragment();
        assetsFragment = new NFTAssetsFragment();
        TokenActivityFragment tokenActivityFragment = new TokenActivityFragment();

        Bundle bundle = new Bundle();
        bundle.putLong(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        bundle.putString(C.EXTRA_ADDRESS, token.getAddress());
        bundle.putParcelable(WALLET, wallet);
        infoFragment.setArguments(bundle);
        assetsFragment.setArguments(bundle);
        tokenActivityFragment.setArguments(bundle);

        List<Pair<String, Fragment>> pages = new ArrayList<>();
        pages.add(0, new Pair<>("Info", infoFragment));
        pages.add(1, new Pair<>("Assets", assetsFragment));
        pages.add(2, new Pair<>("Activity", tokenActivityFragment));

        ScrollControlViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager(), pages));
        setupTabs(viewPager);
    }

    private void setupTabs(ScrollControlViewPager viewPager)
    {
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        tabLayout.setupWithViewPager(viewPager);

        TabUtils.decorateTabLayout(this, tabLayout);

        viewPager.setCurrentItem(1, true);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                switch (tab.getPosition())
                {
                    case 0:
                        showFunctionBar(true);
                        hideMenu();
                        break;
                    case 1:
                        showFunctionBar(false);
                        showMenu();
                        break;
                    default:
                        showFunctionBar(false);
                        hideMenu();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {

            }
        });
    }

    private void showFunctionBar(boolean show)
    {
        if (functionBar == null && !show) return;
        if (BuildConfig.DEBUG || wallet.type != WalletType.WATCH)
        {
            if (functionBar == null) setupFunctionBar();
            functionBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (isGridView)
        {
            switchToListViewMenuItem.setVisible(true);
            switchToGridViewMenuItem.setVisible(false);
        } else
        {
            switchToListViewMenuItem.setVisible(false);
            switchToGridViewMenuItem.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nft_display, menu);
        sendMultipleTokensMenuItem = menu.findItem(R.id.action_send_multiple_tokens);
        switchToGridViewMenuItem = menu.findItem(R.id.action_grid_view);
        switchToListViewMenuItem = menu.findItem(R.id.action_list_view);
        if (token.isBatchTransferAvailable())
        {
            sendMultipleTokensMenuItem.setVisible(true);
            sendMultipleTokensMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
            switchToGridViewMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
            switchToListViewMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        } else
        {
            sendMultipleTokensMenuItem.setVisible(false);
            switchToGridViewMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
            switchToListViewMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_list_view)
        {
            isGridView = false;
            switchToListViewMenuItem.setVisible(false);
            switchToGridViewMenuItem.setVisible(true);
            assetsFragment.showListView();
        } else if (item.getItemId() == R.id.action_grid_view)
        {
            isGridView = true;
            switchToListViewMenuItem.setVisible(true);
            switchToGridViewMenuItem.setVisible(false);
            assetsFragment.showGridView();
        } else if (item.getItemId() == R.id.action_send_multiple_tokens)
        {
            handleTransactionSuccess.launch(viewModel.openSelectionModeIntent(this, token, wallet));
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFunctionBar()
    {
        if (BuildConfig.DEBUG || wallet.type != WalletType.WATCH)
        {
            functionBar = findViewById(R.id.layoutButtons);
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, null);
            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
            functionBar.setVisibility(View.GONE);
        }
    }

    private void hideMenu()
    {
        sendMultipleTokensMenuItem.setVisible(false);
        switchToGridViewMenuItem.setVisible(false);
        switchToListViewMenuItem.setVisible(false);
    }

    private void showMenu()
    {
        if (isGridView)
        {
            switchToListViewMenuItem.setVisible(true);
            switchToGridViewMenuItem.setVisible(false);
        } else
        {
            switchToListViewMenuItem.setVisible(false);
            switchToGridViewMenuItem.setVisible(true);
        }

        if (token.isBatchTransferAvailable())
        {
            sendMultipleTokensMenuItem.setVisible(true);
        }
    }
}