# CodeStreak - LeetCode Progress Tracker

A modern Android application that tracks your LeetCode problem-solving progress with beautiful visualizations, similar to GitHub's contribution graph.

## Features

### 🎯 **Enhanced Problem Distribution Wheel**
- Interactive pie chart with professional styling and animations
- Shows actual problem counts instead of percentages for clarity
- Custom marker tooltips when tapping chart sections
- Enhanced center text with decorative separator line
- Smooth XY animations with easing functions
- Larger chart size (180x180dp) for better visibility
- Custom value positioning outside slices for readability
- LeetCode color-coded difficulty levels:
  - 🟢 Easy: #00B8A3
  - 🟠 Medium: #FFA116  
  - 🔴 Hard: #FF375F

### 📊 **Monthly Contribution Calendar**
- Calendar-style monthly view of daily problem-solving activity
- Navigation arrows to browse through different months
- Day numbers displayed in each cell
- 5-level color intensity based on problems solved per day
- Days of the week header (S M T W T F S)
- Streak tracking (current and longest streak)

### 🎨 **Modern UI Design**
- Dark theme with LeetCode color scheme
- Material Design 3 components
- Smooth animations and transitions
- Card-based layout for clean organization

## Technical Stack

- **Language**: Java
- **UI Framework**: Android Views with Material Design 3
- **Charts**: MPAndroidChart library
- **Networking**: OkHttp3 for API calls
- **Data Storage**: SharedPreferences with Gson for JSON serialization
- **Architecture**: MVP pattern ready for API integration

## Project Structure

```
app/src/main/java/com/example/codestreak/
├── MainActivity.java           # Main UI controller
├── ContributionAdapter.java    # RecyclerView adapter for contribution grid
├── LeetCodeAPI.java           # GraphQL API client for LeetCode
├── LocalDataManager.java     # Local data persistence
└── ProblemSolvedData.java    # Data model for problem statistics
```

## API Integration Ready

The app includes a complete LeetCode GraphQL API client with methods for:

### 📈 **User Profile Data**
```java
leetCodeAPI.getUserProfile(username, new LeetCodeAPI.LeetCodeCallback() {
    @Override
    public void onSuccess(String response) {
        // Handle user profile data
    }
    
    @Override
    public void onError(Exception error) {
        // Handle error
    }
});
```

### 🔍 **Problems by Difficulty**
```java
leetCodeAPI.getProblemsByDifficulty("EASY", callback);
```

### 📅 **Daily Coding Challenge**
```java
leetCodeAPI.getDailyCodingChallenge(callback);
```

## Sample GraphQL Queries

### User Statistics Query
```graphql
{
  matchedUser(username: "your-username") {
    username
    submitStats {
      acSubmissionNum {
        difficulty
        count
      }
    }
    profile {
      ranking
    }
  }
}
```

### Problems Query
```graphql
{
  problemsetQuestionList(
    categorySlug: ""
    limit: 50
    skip: 0
    filters: {difficulty: "EASY"}
  ) {
    total: totalNum
    questions: data {
      title
      titleSlug
      difficulty
      status
      frontendQuestionId
      acRate
    }
  }
}
```

## Color Scheme

### LeetCode Colors
- **Background**: #1A1A1A
- **Cards**: #262626
- **Primary Text**: #FFFFFF
- **Secondary Text**: #A0A0A0
- **Orange Accent**: #FFA116
- **Blue Accent**: #3B82F6

### Contribution Graph Colors
- **Level 0**: #161B22 (No activity)
- **Level 1**: #0E4429 (Low activity) 
- **Level 2**: #006D32 (Medium-low activity)
- **Level 3**: #26A641 (Medium-high activity)
- **Level 4**: #39D353 (High activity)

## Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/ADITYA-SHAKYA04/CODEStreak.git
   cd codeStreak
   ```

2. **Open in Android Studio**
   - Import the project
   - Sync Gradle files
   - Run on device or emulator

3. **API Integration** (Optional)
   - Replace sample data with real LeetCode API calls
   - Add user authentication
   - Implement real-time data syncing

## Future Enhancements

- [ ] LeetCode user authentication
- [ ] Real-time problem sync
- [ ] Daily coding challenges
- [ ] Problem recommendations
- [ ] Achievement system
- [ ] Social features (compare with friends)
- [ ] Export progress reports
- [ ] Custom goal setting
- [ ] Push notifications for daily reminders

## Dependencies

```gradle
implementation 'androidx.appcompat:appcompat:1.7.1'
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.google.code.gson:gson:2.10.1'
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

---

**Note**: This app is not officially affiliated with LeetCode. It uses publicly available GraphQL endpoints for educational purposes.
