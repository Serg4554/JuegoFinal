package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {
    private Spatial mundo, area;
    private BulletAppState estadosFisicos;
    private RigidBodyControl fisicaSuelo, fisicaArea, fisicaPelota, fisicaCanasta;
    private Geometry pelota;
    private Vector3f pPelota, pCanasta;
    private Spatial canasta;
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    public Main() {
        
    }
    
    //Colisiones
    private final PhysicsCollisionListener physicsCollisionListener = new PhysicsCollisionListener() {
        @Override
        public void collision(PhysicsCollisionEvent event) {
            if (event.getNodeA().getName().equals("pelota") && event.getNodeB().getName().equals("suelo")) {
                if(distanciaObjetivo() < 1) {
                    System.out.println("Colisión");
                }
            }
        }
    };

    @Override
    public void simpleInitApp() {
        //Cámara
        this.flyCam.setEnabled(true);
        this.flyCam.setMoveSpeed(30);
        cam.setLocation(new Vector3f(20, 5, 0));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
        
        //Fisica
        estadosFisicos = new BulletAppState();
        stateManager.attach(estadosFisicos);
        
        //Carga mundo
        mundo = assetManager.loadModel("Scenes/escena.j3o");
        mundo.setName("tierra");
        rootNode.attachChild(mundo);
        fisicaSuelo = new RigidBodyControl(0.0f);
        mundo.addControl(fisicaSuelo);
        estadosFisicos.getPhysicsSpace().add(fisicaSuelo);
        fisicaSuelo.setRestitution(0.9f);
        
        //Cielo
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        
        //Ajusta luces
        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(0f, -5f, -1f).normalizeLocal());
        light.setColor(ColorRGBA.White);
        rootNode.addLight(light);
        
        //Carga area
        area = assetManager.loadModel("Models/area.j3o");
        area.setName("suelo");
        rootNode.attachChild(area);
        area.setLocalTranslation(new Vector3f(0, 0, 0));
        fisicaArea = new RigidBodyControl(0.0f);
        area.addControl(fisicaArea);
        estadosFisicos.getPhysicsSpace().add(fisicaArea);
        fisicaArea.setRestitution(0.9f);
        
        //Pelota
        Sphere sphere2 = new Sphere(32, 32, 0.4f);
        TangentBinormalGenerator.generate(sphere2);
        Material mat_bola2 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_bola2.setTexture("DiffuseMap", assetManager.loadTexture("Textures/dirt.jpg"));
        mat_bola2.setBoolean("UseMaterialColors",true);
        mat_bola2.setColor("Diffuse",ColorRGBA.LightGray);
        mat_bola2.setColor("Specular",ColorRGBA.White);
        mat_bola2.setFloat("Shininess", 64f);
        pelota = new Geometry("pelota", sphere2);
        pelota.setMaterial(mat_bola2);
        pelota.setLocalTranslation(new Vector3f(0, 1f, 10));
        rootNode.attachChild(pelota);
        fisicaPelota = new RigidBodyControl(1f);
        pelota.addControl(fisicaPelota);
        estadosFisicos.getPhysicsSpace().add(fisicaPelota);
        fisicaPelota.setRestitution(0.6f);
        fisicaPelota.setMass(0.5f);
        
        //canasta
        canasta = assetManager.loadModel("Models/canasta.j3o");
        canasta.setName("canasta");
        rootNode.attachChild(canasta);
        canasta.setLocalTranslation(new Vector3f(0, 0.5f, -10));
        fisicaCanasta = new RigidBodyControl(0f);
        canasta.addControl(fisicaCanasta);
        estadosFisicos.getPhysicsSpace().add(fisicaCanasta);
        
        //Colisiones
        estadosFisicos.getPhysicsSpace().addCollisionListener(physicsCollisionListener);
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        pPelota = fisicaPelota.getPhysicsLocation();
        pCanasta = fisicaCanasta.getPhysicsLocation();
    }
    
    private double distanciaObjetivo() {
        return Math.sqrt(Math.pow(pPelota.x - pCanasta.x, 2) + Math.pow(pPelota.z - pCanasta.z, 2));
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}
