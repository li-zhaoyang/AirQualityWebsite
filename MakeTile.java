import java.util.*;
import java.lang.Math;
import java.awt.image.*;
import java.io.*;
import javax.imageio.ImageIO;

class MakeTile {
  public static final double[] BOUNDARIES_CHINA = {17, 72, 53, 135};
  public static final double[] BOUNDARIES_BEIJING = {39, 115, 42, 118};
  public static final double MAX_DIST_CHINA = 200;
  public static final double MAX_DIST_BEIJING = 150;
  public static final double GRID_DIFF_BEIJING = 0.05;
  public static final double GRID_DIFF_CHINA = 0.5;
  public static final double IDW_P = 2.6;
  public static final int ALPHA = 180;
  public static final int DEFAULT_AQI = 40;
  public static final int MIN_ZOOM_CHINA = 4;
  public static final int MAX_ZOOM_CHINA = 8;
  public static final int MIN_ZOOM_BEIJING = 9;
  public static final int MAX_ZOOM_BEIJING = 13;
  class Location {
    public double lat;
    public double lon;
    public Location(double inLat, double inLon) {
      lat = inLat;
      lon = inLon;
    }
  }
  class Tile {
    public int x;
    public int y;
    public int zoom;
    public Tile(int xx, int yy, int zoomzoom) {
      x = xx;
      y = yy;
      zoom = zoomzoom;
    }
  }

  class Color {
    public int red;
    public int green;
    public int blue;
    public Color(int r, int g, int b) {
      red = r;
      green = g;
      blue = b;
    }
  }

  public static void main(String[] args) {
    String PATH_TO_FILE = args[0];
    String timeHourNowString = "2018-01-26 151826";
    File file = new File(PATH_TO_FILE + "/web/LatestHour.txt");
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      timeHourNowString = br.readLine();
    } catch(IOException e) {
      e.printStackTrace();
      return ;
    }
    String path = PATH_TO_FILE + "/China/" + timeHourNowString +".csv";
    MakeTile mt = new MakeTile();

    HashMap<Location, Integer> locAQIMap = new HashMap<Location, Integer>();
    ArrayList<Location> locList = new ArrayList<Location>();
    mt.getLocAQIListsFromFile(locAQIMap, locList, path);
    HashMap<Integer, Integer> gridToAQIDictChina = mt.getGridDict(BOUNDARIES_CHINA, GRID_DIFF_CHINA, locAQIMap, locList, MAX_DIST_CHINA);
    HashMap<Integer, Integer> gridToAQIDictBeijing = mt.getGridDict(BOUNDARIES_BEIJING, GRID_DIFF_BEIJING, locAQIMap, locList, MAX_DIST_BEIJING);
    HashMap<Integer, Color> aqiToColorDict = mt.getAQIColorDict();
    String outputPath = PATH_TO_FILE + "/web/tiles/" + timeHourNowString.substring(11) + "/";
    mt.makeAllTiles(MIN_ZOOM_CHINA, MAX_ZOOM_CHINA, BOUNDARIES_CHINA, gridToAQIDictChina, aqiToColorDict, ALPHA, outputPath, GRID_DIFF_CHINA);
    mt.makeAllTiles(MIN_ZOOM_BEIJING, MAX_ZOOM_BEIJING, BOUNDARIES_BEIJING, gridToAQIDictBeijing, aqiToColorDict, ALPHA, outputPath, GRID_DIFF_BEIJING);
  }

  private void getLocAQIListsFromFile(HashMap<Location, Integer> locAQIMap, ArrayList<Location> locList, String path) {
    File file = new File(path);
    String st;
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      while ((st = br.readLine()) != null) {
        // System.out.println(st);
        String[] thisLineList = st.split(",");
        if (thisLineList.length < 4 || thisLineList[3].equals("-"))  continue;
        Location thisLoc = new Location(Double.parseDouble(thisLineList[0]), Double.parseDouble(thisLineList[1]));
        locList.add(thisLoc);
        locAQIMap.put(thisLoc, Integer.parseInt(thisLineList[3]));
      }
    } catch(IOException e) {
      e.printStackTrace();
    }

  }

  private HashMap<Integer, Integer> getGridDict(double[] boundaries, double interval, HashMap<Location, Integer> locAQIMap, ArrayList<Location> locList, double maxDist) {
        double latDown = boundaries[0];
        double lonLeft = boundaries[1];
        double latUp = boundaries[2];
        double lonRight = boundaries[3];
        double lat = latDown;
        HashMap<Integer, Integer> gridDict  = new HashMap<Integer, Integer>();
        while (lat <= latUp) {
          double lon = lonLeft;
          while (lon <= lonRight) {
            gridDict.put(latLonToKeyInGridDict(lat, lon), predictAQI(new Location(lat, lon), locAQIMap, locList, maxDist));
            System.out.println("grid: " + lat + ", " + lon);
            lon += interval;
          }
          lat += interval;
        }
        return gridDict;
  }

  private int latLonToKeyInGridDict(double lat, double lon) {
    return (int) Math.round(lon * 20 * 360 * 20 + lat * 20);
  }

  private int predictAQI(Location location, HashMap<Location, Integer> locAQIMap, ArrayList<Location> locList, double maxDist) {
    double p = IDW_P;
    double up = 0;
    double down = 0;
    for (int i = 0; i < locList.size(); i++) {
      Location thisLoc = locList.get(i);
      int thisAQI = locAQIMap.get(thisLoc);
      double thisDis = dis(thisLoc, location);
      if (thisDis < 1)  return thisAQI ;
      if (thisDis > maxDist)  continue;
      double w = Math.pow(thisDis, -p);
      up += w * thisAQI;
      down += w;
    }
    if (down == 0)  return DEFAULT_AQI;
    return (int) (up / down);
  }

  private double dis(Location location1, Location location2) {
    double r = 6371;
    if (location1.lat == location2.lat && location1.lon == location2.lon)
      return 0;
    else if (((location1.lon == 180 + location2.lon) || (location2.lon == 180 + location1.lon)) && location1.lat + location2.lat == 0)
      return r * Math.PI;
    else
      return r * Math.acos(Math.sin(location1.lat * Math.PI / 180) * Math.sin(location2.lat * Math.PI / 180) + Math.cos(location1.lat * Math.PI / 180) * Math.cos(location2.lat * Math.PI / 180) * Math.cos(Math.abs(location1.lon - location2. lon) * Math.PI / 180));
  }

  private HashMap<Integer, Color> getAQIColorDict() {
    HashMap<Integer, Color> AQIColorDict = new HashMap<Integer, Color>();
    AQIColorDict.put(0  , new Color(0, 255, 0));
    AQIColorDict.put(25 , new Color(0, 228, 0));
    AQIColorDict.put(75 , new Color(255, 255, 0));
    AQIColorDict.put(125, new Color(255, 126, 0));
    AQIColorDict.put(175, new Color(255, 0, 0));
    AQIColorDict.put(250, new Color(153, 0, 76));
    AQIColorDict.put(400, new Color(126, 0, 35));
    AQIColorDict.put(500, new Color(0, 0, 0));
    int[] splitList = {0, 25, 75, 125, 175, 250, 400, 500};
    for (int i = 1; i < 500; i++) {
      if (i == 25 || i == 75 || i == 125 || i == 175 || i == 250 || i == 400) continue;
      for (int j = 0; j < splitList.length; j++) {
        if (i < splitList[j]) {
          AQIColorDict.put(i, interpolateColor(i, splitList[j - 1], splitList[j], AQIColorDict.get(splitList[j - 1]), AQIColorDict.get(splitList[j])));
          break;
        }
      }
    }
    return AQIColorDict;
  }

  private Color interpolateColor(int key, int keyLess, int keyMore, Color colorLess, Color colorMore) {
    int red = colorLess.red + (int) (key - keyLess) * (colorMore.red - colorLess.red) / (keyMore - keyLess);
    int green = colorLess.green + (int) (key - keyLess) * (colorMore.green - colorLess.green) / (keyMore - keyLess);
    int blue = colorLess.blue + (int) (key - keyLess) * (colorMore.blue - colorLess.blue) / (keyMore - keyLess);
    return new Color(red, green, blue);
  }

  private int[] deg2num(double lat_deg, double lon_deg, int zoom){
    double lat_rad = Math.toRadians(lat_deg);
    int n = 1 << zoom;
    int xtile = (int) ((lon_deg + 180.0) / 360.0 * n);
    int ytile = (int) ((1.0 - Math.log(Math.tan(lat_rad) + (1 / Math.cos(lat_rad))) / Math.PI) / 2.0 * n);
    int[] ans = new int[2];
    ans[0] = xtile;
    ans[1] = ytile;
    return ans;
  }

  private void makeAllTiles(int minZoom, int maxZoom, double[] boundaries, HashMap<Integer, Integer> gridToAQIDict, HashMap<Integer, Color> aqiToColorDict, int alpha, String path, double gridDiff) {
    double latDown = boundaries[0];
    double lonLeft = boundaries[1];
    double latUp = boundaries[2];
    double lonRight = boundaries[3];
    for (int zoom = minZoom; zoom < maxZoom + 1; zoom++) {
      int[] upLeft = deg2num(latUp, lonLeft, zoom);
      int[] downRight = deg2num(latDown, lonRight, zoom);
      for (int x = upLeft[0]; x < downRight[0] + 1; x++) {
        for (int y = upLeft[1]; y < downRight[1] + 1; y++) {
          generateImage(gridToAQIDict, aqiToColorDict, new Tile(x, y, zoom), alpha, path, gridDiff);
        }
      }
    }
  }

  private void generateImage(HashMap<Integer, Integer> gridToAQIDict, HashMap<Integer, Color> aqiToColorDict, Tile tile, int alpha, String path, double gridDiff) {
    int[] array = new int[256 * 256 * 4];
    int devider = (int) (1.0 / gridDiff);
    for (int x = 0; x < 256; x++) {
      for (int y = 0; y < 256; y++) {
        Location tileLoc = tileLocation(new Tile(x + tile.x * 256, y + tile.y * 256, tile.zoom + 8));
        double smallerGridLat = ((double) (int) (tileLoc.lat * devider)) / (double) devider;
        double smallerGridLon = ((double) (int) (tileLoc.lon * devider)) / (double) devider;

        double xInSquare = tileLoc.lat - smallerGridLat;
        double yInSquare = tileLoc.lon - smallerGridLon;
        int d00 = getAQIFromKey(latLonToKeyInGridDict(smallerGridLat, smallerGridLon), gridToAQIDict);
        int d01 = getAQIFromKey(latLonToKeyInGridDict(smallerGridLat, smallerGridLon + gridDiff), gridToAQIDict);
        int d10 = getAQIFromKey(latLonToKeyInGridDict(smallerGridLat + gridDiff, smallerGridLon), gridToAQIDict);
        int d11 = getAQIFromKey(latLonToKeyInGridDict(smallerGridLat + gridDiff, smallerGridLon + gridDiff), gridToAQIDict);
        int aqi = (int) (bilinearInterpolation(xInSquare * devider, yInSquare * devider, d00, d01, d10, d11));
        if(aqi > 500)
            aqi = 500;
        if (aqi < 0)
            aqi = 0;
        Color color = aqiToColorDict.get(aqi);
        array[(y * 256 + x) * 4] = color.red;
        array[(y * 256 + x) * 4 + 1] = color.green;
        array[(y * 256 + x) * 4 + 2] = color.blue;
        array[(y * 256 + x) * 4 + 3] = ALPHA;
      }
    }
    BufferedImage thisImg = getImageFromArray(array, 256, 256);
    try {
      File outputfile = new File(path + tile.zoom + '/'+ tile.x+ '-' + tile.y +".png");
      ImageIO.write(thisImg, "png", outputfile);
      System.out.println("made tile(zoom, x, y): " + tile.zoom + " " + tile.x + " " + tile.y);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Location tileLocation(Tile tile) {
    int n = 1 << tile.zoom;
    double lon_deg = tile.x / (float) n * 360.0 - 180.0;
    double lat_rad = Math.atan(Math.sinh(Math.PI * (1 - 2 * tile.y / (float) n)));
    double lat_deg = lat_rad * 180.0 / Math.PI;
    return new Location(lat_deg, lon_deg);
  }

  private int getAQIFromKey(int key, HashMap<Integer, Integer> gridToAQIDict) {
    if (gridToAQIDict.containsKey(key)) {
      return gridToAQIDict.get(key);
    }
    return DEFAULT_AQI;
  }

  private double bilinearInterpolation(double x, double y, double d00, double d01, double d10, double d11) {
    return d00 * (1 - x) * (1 - y) + d10 * x * (1 - y) + d01 * (1 - x) * y + d11 * x * y;
  }

  private BufferedImage getImageFromArray(int[] pixels, int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    WritableRaster raster = image.getRaster();
    raster.setPixels(0, 0, width, height, pixels);
    image.setData(raster);
    return image;
  }

}
