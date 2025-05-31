import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';
import 'package:usage_stats/usage_stats.dart'; // Import usage_stats

void main() {
  runApp(const DigitalWellbeingApp());
}

class DigitalWellbeingApp extends StatelessWidget {
  const DigitalWellbeingApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Digital Wellbeing',
      theme: ThemeData.dark().copyWith(
        primaryColor: Colors.blue, // Changed from primarySwatch
        scaffoldBackgroundColor: const Color(0xFF0A0A0A),
        cardColor: const Color(0xFF1A1A1A),
        textTheme: ThemeData.dark().textTheme.copyWith( // Correctly extend the dark theme's textTheme
              bodyLarge: const TextStyle(color: Colors.white),
              bodyMedium: const TextStyle(color: Colors.white70),
            ),
      ),
      home: const MainScreen(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  _MainScreenState createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> with TickerProviderStateMixin {
  int _selectedIndex = 0;
  late AnimationController _glowController;
  late Animation<double> _glowAnimation;
  
  List<AppUsage> appUsageList = [];
  Timer? _usageTimer;
  Timer? _breakTimer;
  String motivationMessage = "Stay focused and take breaks!";
bool _settingsBreakReminders = true;
  bool _settingsUsageTracking = true;
  bool _settingsSocialMediaLimits = true;
  bool _settingsDailyMotivation = true;
  
  // Social media apps to monitor
  final List<String> socialMediaApps = [
    'Instagram',
    'Snapchat',
    'TikTok',
    'Facebook',
    'Twitter',
    'YouTube',
    'WhatsApp'
  ];

  @override
  void initState() {
    super.initState();
    _glowController = AnimationController(
      duration: Duration(seconds: 2),
      vsync: this,
    )..repeat(reverse: true);
    
    _glowAnimation = Tween<double>(
      begin: 0.3,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _glowController,
      curve: Curves.easeInOut,
    ));
    
    _initializeApp();
  }

  void _initializeApp() {
    _loadAppUsage();
    _startUsageMonitoring();
    _fetchMotivationMessage();
  }

  List<AppUsage> _getSampleAppUsage() {
    return [
      AppUsage('Instagram', const Duration(hours: 2, minutes: 30), true),
      AppUsage('Snapchat', const Duration(hours: 1, minutes: 45), true),
      AppUsage('TikTok', const Duration(hours: 3, minutes: 15), true),
      AppUsage('Facebook', const Duration(hours: 1, minutes: 20), true),
      AppUsage('YouTube', const Duration(hours: 2, minutes: 45), true),
      AppUsage('WhatsApp', const Duration(minutes: 45), false),
      AppUsage('Chrome', const Duration(hours: 1, minutes: 30), false),
    ];
  }

  void _loadSampleAppUsage() {
    if (mounted) {
      setState(() {
        appUsageList = _getSampleAppUsage();
      });
    }
  }

  Future<void> _loadAppUsage() async {
    try {
      // Check and request permission
      bool? isGranted = await UsageStats.checkUsagePermission();
      if (isGranted == null || !isGranted) {
        print("Usage permission initially not granted. Requesting...");
        await UsageStats.grantUsagePermission();
        // Re-check permission after requesting
        isGranted = await UsageStats.checkUsagePermission();
        if (isGranted == null || !isGranted) {
          print("Usage permission not granted by user after request.");
          _loadSampleAppUsage(); // Load sample data if permission is still not granted
          return;
        }
        print("Usage permission granted by user.");
      } else {
        print("Usage permission already granted.");
      }

      DateTime endDate = DateTime.now();
      DateTime startDate = endDate.subtract(const Duration(days: 1));
      print("Querying usage stats from $startDate to $endDate");

      List<UsageInfo> usageStats = await UsageStats.queryUsageStats(startDate, endDate);
      print("Raw usageStats count: ${usageStats.length}");
      for (var usage in usageStats) {
        print("Raw UsageInfo: Pkg: ${usage.packageName}, Time: ${usage.totalTimeInForeground}, First: ${usage.firstTimeStamp}, Last: ${usage.lastTimeStamp}, LastUsed: ${usage.lastTimeUsed}");
      }
      
      // Filter out entries with null, empty, or zero foreground time
      usageStats.removeWhere((usage) {
        final timeInForeground = usage.totalTimeInForeground;
        if (timeInForeground == null || timeInForeground.isEmpty) return true;
        final parsedTime = int.tryParse(timeInForeground);
        return parsedTime == null || parsedTime == 0;
      });
      print("Filtered usageStats count: ${usageStats.length}");


      List<AppUsage> realAppUsage = [];
      for (var usage in usageStats) {
        if (usage.packageName != null && usage.totalTimeInForeground != null) {
          String appName = usage.packageName!;
          if (appName.contains('.')) {
            appName = appName.substring(appName.lastIndexOf('.') + 1);
            appName = appName[0].toUpperCase() + appName.substring(1);
          }
          
          // A more robust check for social media apps
          bool isSocial = socialMediaApps.any((socialApp) =>
              usage.packageName!.toLowerCase().contains(socialApp.toLowerCase()) ||
              appName.toLowerCase().contains(socialApp.toLowerCase()));

          realAppUsage.add(AppUsage(
            appName,
            Duration(milliseconds: int.parse(usage.totalTimeInForeground!)),
            isSocial
          ));
        }
      }
      
      realAppUsage.sort((a, b) => b.usage.compareTo(a.usage));
      print("Processed realAppUsage count: ${realAppUsage.length}");
      if (realAppUsage.isEmpty) {
        print("No real app usage data processed, falling back to sample data.");
      }


      if (mounted) {
        setState(() {
          appUsageList = realAppUsage.isNotEmpty ? realAppUsage : _getSampleAppUsage();
        });
      }

    } catch (e) {
      print("Error loading app usage: $e");
      _loadSampleAppUsage(); // Load sample data in case of any error
    }
  }

  void _startUsageMonitoring() {
    _usageTimer = Timer.periodic(Duration(minutes: 1), (timer) {
      _checkBreakTime();
    });
  }

  void _checkBreakTime() {
    for (AppUsage app in appUsageList) {
      if (app.isSocialMedia && app.usage.inMinutes > 0) {
        if (app.usage.inMinutes % 15 == 0) { // Every 15 minutes
          _showBreakNotification(app.appName);
        }
      }
    }
  }

  void _showBreakNotification(String appName) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: Color(0xFF1A1A1A),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
            side: BorderSide(color: Colors.cyanAccent.withOpacity(0.5)),
          ),
          title: Text(
            '⏰ Break Time!',
            style: TextStyle(color: Colors.cyanAccent),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'You\'ve been using $appName for a while.',
                style: TextStyle(color: Colors.white70),
              ),
              SizedBox(height: 10),
              Text(
                motivationMessage,
                style: TextStyle(
                  color: Colors.greenAccent,
                  fontStyle: FontStyle.italic,
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: Text('5 Min Break', style: TextStyle(color: Colors.cyanAccent)),
            ),
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: Text('10 Min Break', style: TextStyle(color: Colors.cyanAccent)),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                _forceAppExit(appName);
              },
              child: Text('Exit App', style: TextStyle(color: Colors.redAccent)),
            ),
          ],
        );
      },
    );
  }

  void _forceAppExit(String appName) {
    // In real implementation, use platform channels to close specific apps
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('$appName session ended. Take a break!'),
        backgroundColor: Colors.greenAccent.withOpacity(0.8),
      ),
    );
  }

  Future<void> _fetchMotivationMessage() async {
    // For now, using predefined motivational messages
    // Add http dependency in pubspec.yaml to use API calls
    List<String> motivationalQuotes = [
      "Take breaks to refresh your mind and boost productivity!",
      "Balance is not something you find, it's something you create.",
      "Your mental health is just as important as your physical health.",
      "Small breaks lead to big breakthroughs.",
      "Disconnect to reconnect with yourself.",
      "Every moment of rest is an investment in your future self.",
      "Quality time with yourself is never time wasted.",
    ];
    
    setState(() {
      motivationMessage = (motivationalQuotes..shuffle()).first;
    });
  }

  @override
  void dispose() {
    _glowController.dispose();
    _usageTimer?.cancel();
    _breakTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Color(0xFF0A0A0A),
              Color(0xFF1A1A2E),
              Color(0xFF0A0A0A),
            ],
          ),
        ),
        child: SafeArea(
          child: IndexedStack(
            index: _selectedIndex,
            children: [
              _buildDashboard(),
              _buildUsageStats(),
              _buildSettings(),
            ],
          ),
        ),
      ),
      bottomNavigationBar: _buildBottomNavBar(),
    );
  }

  Widget _buildDashboard() {
    return SingleChildScrollView(
      padding: EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeader(),
          SizedBox(height: 30),
          _buildMotivationCard(),
          SizedBox(height: 30),
          _buildQuickStats(),
          SizedBox(height: 30),
          _buildSocialMediaApps(),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return AnimatedBuilder(
      animation: _glowAnimation,
      builder: (context, child) {
        return Container(
          padding: EdgeInsets.all(20),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(20),
            gradient: LinearGradient(
              colors: [
                Colors.cyanAccent.withOpacity(0.1),
                Colors.blueAccent.withOpacity(0.1),
              ],
            ),
            boxShadow: [
              BoxShadow(
                color: Colors.cyanAccent.withOpacity(_glowAnimation.value * 0.3),
                blurRadius: 20,
              ),
            ],
          ),
          child: Row(
            children: [
              Icon(
                Icons.psychology,
                color: Colors.cyanAccent,
                size: 40,
              ),
              SizedBox(width: 15),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Digital Wellbeing',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  Text(
                    'Balance your digital life',
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 14,
                    ),
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildMotivationCard() {
    return Container(
      padding: EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(15),
        gradient: LinearGradient(
          colors: [
            Colors.greenAccent.withOpacity(0.1),
            Colors.tealAccent.withOpacity(0.1),
          ],
        ),
        border: Border.all(
          color: Colors.greenAccent.withOpacity(0.3),
          width: 1,
        ),
      ),
      child: Column(
        children: [
          Icon(
            Icons.lightbulb_outline,
            color: Colors.greenAccent,
            size: 30,
          ),
          SizedBox(height: 10),
          Text(
            'Daily Motivation',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.greenAccent,
            ),
          ),
          SizedBox(height: 10),
          Text(
            motivationMessage,
            textAlign: TextAlign.center,
            style: TextStyle(
              color: Colors.white70,
              fontSize: 16,
              fontStyle: FontStyle.italic,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickStats() {
    final totalUsage = appUsageList.fold<Duration>(
      Duration.zero,
      (total, app) => total + app.usage,
    );
    
    final socialMediaUsage = appUsageList
        .where((app) => app.isSocialMedia)
        .fold<Duration>(Duration.zero, (total, app) => total + app.usage);

    return Row(
      children: [
        Expanded(child: _buildStatCard('Total Screen Time', _formatDuration(totalUsage), Icons.phone_android, Colors.blueAccent)),
        SizedBox(width: 15),
        Expanded(child: _buildStatCard('Social Media', _formatDuration(socialMediaUsage), Icons.group, Colors.pinkAccent)),
      ],
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, Color color) {
    return Container(
      padding: EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(15),
        color: Color(0xFF1A1A1A),
        border: Border.all(
          color: color.withOpacity(0.3),
          width: 1,
        ),
        boxShadow: [
          BoxShadow(
            color: color.withOpacity(0.1),
            blurRadius: 10,
          ),
        ],
      ),
      child: Column(
        children: [
          Icon(icon, color: color, size: 30),
          SizedBox(height: 10),
          Text(
            value,
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          Text(
            title,
            style: TextStyle(
              color: Colors.white70,
              fontSize: 12,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSocialMediaApps() {
    final socialApps = appUsageList.where((app) => app.isSocialMedia).toList();
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Social Media Usage',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
        SizedBox(height: 15),
        ...socialApps.map((app) => _buildAppUsageCard(app)).toList(),
      ],
    );
  }

  Widget _buildAppUsageCard(AppUsage app) {
    return Container(
      margin: EdgeInsets.only(bottom: 10),
      padding: EdgeInsets.all(15),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        color: Color(0xFF1A1A1A),
        border: Border.all(
          color: Colors.redAccent.withOpacity(0.3),
          width: 1,
        ),
      ),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: Colors.redAccent.withOpacity(0.2),
            child: Text(
              app.appName[0],
              style: TextStyle(
                color: Colors.redAccent,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          SizedBox(width: 15),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  app.appName,
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                Text(
                  _formatDuration(app.usage),
                  style: TextStyle(
                    color: Colors.white70,
                    fontSize: 14,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: () => _showBreakNotification(app.appName),
            icon: Icon(
              Icons.timer,
              color: Colors.orangeAccent,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildUsageStats() {
    return Padding(
      padding: EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Usage Statistics',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          SizedBox(height: 20),
          Expanded(
            child: ListView.builder(
              itemCount: appUsageList.length,
              itemBuilder: (context, index) {
                final app = appUsageList[index];
                return _buildDetailedAppCard(app);
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailedAppCard(AppUsage app) {
    return Container(
      margin: EdgeInsets.only(bottom: 15),
      padding: EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(15),
        color: Color(0xFF1A1A1A),
        border: Border.all(
          color: app.isSocialMedia 
              ? Colors.redAccent.withOpacity(0.3)
              : Colors.blueAccent.withOpacity(0.3),
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CircleAvatar(
                backgroundColor: app.isSocialMedia 
                    ? Colors.redAccent.withOpacity(0.2)
                    : Colors.blueAccent.withOpacity(0.2),
                child: Text(
                  app.appName[0],
                  style: TextStyle(
                    color: app.isSocialMedia ? Colors.redAccent : Colors.blueAccent,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              SizedBox(width: 15),
              Expanded(
                child: Text(
                  app.appName,
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
              if (app.isSocialMedia)
                Chip(
                  label: Text('Social'),
                  backgroundColor: Colors.redAccent.withOpacity(0.2),
                  labelStyle: TextStyle(color: Colors.redAccent, fontSize: 12),
                ),
            ],
          ),
          SizedBox(height: 15),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Usage Time: ${_formatDuration(app.usage)}',
                style: TextStyle(color: Colors.white70),
              ),
              if (app.isSocialMedia)
                ElevatedButton(
                  onPressed: () => _showBreakNotification(app.appName),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.orangeAccent.withOpacity(0.2),
                    foregroundColor: Colors.orangeAccent,
                  ),
                  child: Text('Take Break'),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSettings() {
    return Padding(
      padding: EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Settings',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          SizedBox(height: 30),
          _buildSettingsTile(
            'Break Reminders',
            'Get notified to take breaks',
            Icons.notifications_active,
            Colors.greenAccent,
            _settingsBreakReminders,
            (newValue) => setState(() => _settingsBreakReminders = newValue),
          ),
          _buildSettingsTile(
            'Usage Tracking',
            'Monitor app usage time',
            Icons.track_changes,
            Colors.blueAccent,
            _settingsUsageTracking,
            (newValue) => setState(() => _settingsUsageTracking = newValue),
          ),
          _buildSettingsTile(
            'Social Media Limits',
            'Set time limits for social apps',
            Icons.timer_off,
            Colors.redAccent,
            _settingsSocialMediaLimits,
            (newValue) => setState(() => _settingsSocialMediaLimits = newValue),
          ),
          _buildSettingsTile(
            'Daily Motivation',
            'Receive daily motivational quotes',
            Icons.lightbulb,
            Colors.yellowAccent,
            _settingsDailyMotivation,
            (newValue) => setState(() => _settingsDailyMotivation = newValue),
          ),
        ],
      ),
    );
  }

  Widget _buildSettingsTile(String title, String subtitle, IconData icon, Color color, bool value, ValueChanged<bool> onChanged) {
    return Container(
      margin: const EdgeInsets.only(bottom: 15),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(15),
        color: Color(0xFF1A1A1A),
        border: Border.all(
          color: color.withOpacity(0.3),
          width: 1,
        ),
      ),
      child: Row(
        children: [
          Icon(icon, color: color, size: 30),
          const SizedBox(width: 15),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                Text(
                  subtitle,
                  style: const TextStyle(
                    color: Colors.white70,
                    fontSize: 14,
                  ),
                ),
              ],
            ),
          ),
          Switch(
            value: value,
            onChanged: onChanged,
            activeColor: color,
          ),
        ],
      ),
    );
  }

  Widget _buildBottomNavBar() {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF1A1A1A),
        borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
        boxShadow: [
          BoxShadow(
            color: Colors.cyanAccent.withOpacity(0.1),
            blurRadius: 20,
          ),
        ],
      ),
      child: BottomNavigationBar(
        currentIndex: _selectedIndex,
        onTap: (index) => setState(() => _selectedIndex = index),
        backgroundColor: Colors.transparent,
        elevation: 0,
        selectedItemColor: Colors.cyanAccent,
        unselectedItemColor: Colors.white54,
        type: BottomNavigationBarType.fixed,
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.dashboard),
            label: 'Dashboard',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.bar_chart),
            label: 'Stats',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings),
            label: 'Settings',
          ),
        ],
      ),
    );
  }

  String _formatDuration(Duration duration) {
    String twoDigits(int n) => n.toString().padLeft(2, "0");
    String twoDigitMinutes = twoDigits(duration.inMinutes.remainder(60));
    return "${twoDigits(duration.inHours)}:$twoDigitMinutes";
  }
}

class AppUsage {
  final String appName;
  final Duration usage;
  final bool isSocialMedia;

  AppUsage(this.appName, this.usage, this.isSocialMedia);
}