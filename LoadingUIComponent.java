// Loading and UI Management Component extracted from MainActivity
public class LoadingUIComponent {
    
    // Loading UI components
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout loadingOverlay;
    private ScrollView skeletonLayout;
    private LinearLayout mainContentLayout;
    private ProgressBar loadingProgressBar;
    
    public void setupLoadingUI() {
        // Configure pull-to-refresh
        swipeRefreshLayout.setColorSchemeColors(
            Color.parseColor("#FFA116"), // LeetCode orange
            Color.parseColor("#00B8A3"), // Easy green
            Color.parseColor("#FFC01E")  // Medium yellow
        );
        
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });
    }
    
    public void showLoadingState() {
        loadingOverlay.setVisibility(View.VISIBLE);
        skeletonLayout.setVisibility(View.GONE);
        mainContentLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }
    
    public void showSkeletonState() {
        loadingOverlay.setVisibility(View.GONE);
        skeletonLayout.setVisibility(View.VISIBLE);
        mainContentLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }
    
    public void showContentState() {
        loadingOverlay.setVisibility(View.GONE);
        skeletonLayout.setVisibility(View.GONE);
        mainContentLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(false);
    }
    
    public void refreshData() {
        // Clear existing data and cache
        // submissionCalendarData = null;
        // monthsWithDataCache = null;
        
        // Show skeleton while refreshing
        showSkeletonState();
        
        // Refresh data after a brief delay
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // fetchLeetCodeData();
                onDataRefreshComplete();
            }
        }, 300);
    }
    
    // Interface for callback when data refresh is complete
    public interface DataRefreshCallback {
        void onDataRefreshComplete();
    }
    
    private DataRefreshCallback refreshCallback;
    
    public void setRefreshCallback(DataRefreshCallback callback) {
        this.refreshCallback = callback;
    }
    
    private void onDataRefreshComplete() {
        if (refreshCallback != null) {
            refreshCallback.onDataRefreshComplete();
        }
    }
}
