import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Scanner;

public class Viewer {
    public static void main(String[] args) {

        //main frame to hold content
        JFrame jFrame = new JFrame();
        Container container = jFrame.getContentPane();
        container.setLayout(new BorderLayout()); 

        //sliders to control rotation
        JSlider horizontalSlider = new JSlider(0, 360, 180);
        container.add(horizontalSlider, BorderLayout.SOUTH);

        JSlider verticalSlider = new JSlider(JSlider.VERTICAL, -90, 90, 0);
        container.add(verticalSlider, BorderLayout.EAST); 

        //panel in which all graphics are rendered
        JPanel renderer = new JPanel() {
            public void paintComponent(Graphics g) {

                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(new Color(20,20,25));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                //g2d.translate(getWidth()/2, getHeight()/2); //move graphics to centre of the JPanel
                g2d.setColor(Color.WHITE);

                //Simple Render
                /*for (Triangle t : triangleList) {

                    //follow path of each triangle and draw it on the graphics
                    Path2D path = new Path2D.Double();
                    path.moveTo(t.v1.x, t.v1.y);
                    path.lineTo(t.v2.x, t.v2.y);
                    path.lineTo(t.v3.x, t.v3.y);
                    path.closePath();
                    g2d.draw(path);
                }*/

                //applying XZ rotation matrix for horizontalslider radian value
                double horizontal = Math.toRadians(horizontalSlider.getValue());
                Matrix3 horizontalTransformation = new Matrix3(new double[] {
                    Math.cos(horizontal), 0, Math.sin(horizontal),
                    0, 1, 0,
                    -Math.sin(horizontal), 0, Math.cos(horizontal)
                });

                //applying YZ rotation matrix for verticalslider radian value
                double vertical = Math.toRadians(verticalSlider.getValue());
                Matrix3 verticalTranformation = new Matrix3(new double[] {
                    1, 0, 0,
                    0, Math.cos(vertical), Math.sin(vertical),
                    0, -Math.sin(vertical), Math.cos(vertical)
                });

                ArrayList<Triangle> triangleList = createTriangleList();
                ArrayList<Cube> cubeList = createCubeList();
                //multiply both matrices into 1
                Matrix3 transformed = horizontalTransformation.multiply(verticalTranformation);
                //image rasterization into pixels
                BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

                double[] zBuffer = new double[image.getWidth() * image.getHeight()];
                // initialize array with extremely far away depths
                for (int z = 0; z < zBuffer.length; z++) {
                    zBuffer[z] = Double.NEGATIVE_INFINITY;
                }
                    for (Triangle t : triangleList) {
                        //apply transformation to each vertex
                        Vertex v1 = transformed.transform(t.v1);
                        Vertex v2 = transformed.transform(t.v2);
                        Vertex v3 = transformed.transform(t.v3);
    
                        /* when using graphics 2d
                        //draw path connecting each vertex
                        Path2D path = new Path2D.Double();
                        path.moveTo(v1.x, v1.y);
                        path.lineTo(v2.x, v2.y);
                        path.lineTo(v3.x, v3.y);
                        path.closePath();
                        g2d.draw(path); */
    
                        //manual computation
                        v1.x += getWidth() / 2;
                        v1.y += getHeight() / 2;
                        v2.x += getWidth() / 2;
                        v2.y += getHeight() / 2;
                        v3.x += getWidth() / 2;
                        v3.y += getHeight() / 2;
    
                        //calculating minimum and maximum x,y coordinates of bounding box
                        int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
                        int maxX = (int) Math.min(image.getWidth() - 1, Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
                        int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
                        int maxY = (int) Math.min(image.getHeight() - 1,Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));
    
                        //calculating area using determinant method
                        double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);
    
                        //calculating normal vector of triangle
                        double ABx = v2.x - v1.x;
                        double ABy = v2.y - v1.y;
                        double ABz = v2.z - v1.z;
                        double ACx = v3.x - v1.x;
                        double ACy = v3.y - v1.y;
                        double ACz = v3.z - v1.z;
    
                        double crossProductX = ABy * ACz - ABz * ACy;
                        double crossProductY = ABz * ACx - ABx * ACz;
                        double crossProductZ = ABx * ACy - ABy * ACx;
    
                        // Create a new vertex to represent the resulting cross product AB x AC
                        Vertex norm = new Vertex(crossProductX, crossProductY, crossProductZ);
                        double normalLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
                        norm.x /= normalLength;
                        norm.y /= normalLength;
                        norm.z /= normalLength;
    
                        //iterating through each pixel and setting it to the appropriate color
                        for (int y = minY; y <= maxY; y++) {
                            for (int x = minX; x <= maxX; x++) {
                                //calculation of baycentric coordinates and how much each vertex contributes to position of pixel
                                double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
                                double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
                                double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;
    
                                //checks if baycentric coordinates are within triangle
                                if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1) {
                                    double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z;
                                    //calculates the index of the current pixel in the z buffer array
                                    int zIndex = y * image.getWidth() + x;
                                    //compare depth of current pixel with depth of pixel stored in z buffer
                                    if (zBuffer[zIndex] < depth) {
                                        //set shading and update depth
                                        Color shadedColor = getShade(t.color, Math.abs((float) norm.z));
                                        image.setRGB(x, y, shadedColor.getRGB());
                                        zBuffer[zIndex] = depth;
                                    }
                                }  
                            }
                        }
                        
                    }

                g2d.drawImage(image, 0, 0, null); 
        }
    };

        //everytime sliders are changed, the renderer is repainted
        horizontalSlider.addChangeListener(event -> renderer.repaint());
        verticalSlider.addChangeListener(event -> renderer.repaint());
        
        container.add(renderer, BorderLayout.CENTER);
        jFrame.setSize(400,400);
        jFrame.setVisible(true);

    }

    public static ArrayList<Triangle> createTriangleList() {

        //creates TetraHedron at 0,0,0
        ArrayList<Triangle> triangleList = new ArrayList<>();
        int d = 100;
        triangleList.add(new Triangle(new Vertex(d, d, d),
                      new Vertex(-d, -d, d),
                      new Vertex(-d, d, -d),
                      Color.ORANGE));
        triangleList.add(new Triangle(new Vertex(d, d, d),
                      new Vertex(-d, -d, d),
                      new Vertex(d, -d, -d),
                      Color.GRAY));
        triangleList.add(new Triangle(new Vertex(-d, d, -d),
                      new Vertex(d, -d, -d),
                      new Vertex(d, d, d),
                      Color.MAGENTA));
        triangleList.add(new Triangle(new Vertex(-d, d, -d),
                      new Vertex(d, -d, -d),
                      new Vertex(-d, -d, d),
                      Color.BLUE));
        return triangleList;
    }

    public static ArrayList<Cube> createCubeList() {

        //creates Cube at 0,0,0
        ArrayList<Cube> cubeList = new ArrayList<>();
        int d = 100;
        cubeList.add(new Cube(
            new Vertex(d, d, d),
            new Vertex(-d, d, d),
            new Vertex(-d, -d, d),
            new Vertex(d, -d, d),
            Color.ORANGE));

        // Back Face
        cubeList.add(new Cube(
            new Vertex(d, d, -d),
            new Vertex(d, -d, -d),
            new Vertex(-d, -d, -d),
            new Vertex(-d, d, -d),
            Color.BLUE));

        // Top Face
        cubeList.add(new Cube(
            new Vertex(d, d, d),
            new Vertex(-d, d, d),
            new Vertex(-d, d, -d),
            new Vertex(d, d, -d),
            Color.GREEN));

        // Bottom Face
        cubeList.add(new Cube(
            new Vertex(d, -d, d),
            new Vertex(d, -d, -d),
            new Vertex(-d, -d, -d),
            new Vertex(-d, -d, d),
            Color.MAGENTA));

        // Left Face
        cubeList.add(new Cube(
            new Vertex(-d, d, d),
            new Vertex(-d, d, -d),
            new Vertex(-d, -d, -d),
            new Vertex(-d, -d, d),
            Color.RED));

        // Right Face
        cubeList.add(new Cube(
            new Vertex(d, d, d),
            new Vertex(d, -d, d),
            new Vertex(d, -d, -d),
            new Vertex(d, d, -d),
            Color.YELLOW));
        return cubeList;
    }

    public static Color getShade(Color color, double shade) {

        //converting to rgb
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(redLinear, 1 / 2.4);
        int green = (int) Math.pow(greenLinear, 1 / 2.4);
        int blue = (int) Math.pow(blueLinear, 1 / 2.4);

        return new Color(red, green, blue);
    }
}
