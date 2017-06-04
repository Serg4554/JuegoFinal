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
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.M5P;
import weka.core.Attribute;
import weka.core.Debug;
import weka.core.Instance;
import weka.core.Instances;

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
    private boolean lanzando, finLanzamiento;
    private Classifier conocimiento = null;
    private Instances casosEntrenamiento = null;
    private Instance casoAdecidir;
    private int maximoNumeroCasosEntrenamiento = 300;
    
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
                    System.out.println("Canasta!");
                    
                }
                
                if(!finLanzamiento && lanzando){
                    double valorReal = errorCometido();
                    casoAdecidir.setClassValue(valorReal);
                    casosEntrenamiento.add(casoAdecidir);
                    for (int i = 0; i < casosEntrenamiento.numInstances() - maximoNumeroCasosEntrenamiento; i++) {
                        casosEntrenamiento.delete(0);  //Hay muchos ejemplos borrar el más antiguo
                    }
                
                    nuevaPelota();
                    lanzando = false;
                    finLanzamiento = true;
                    System.out.println(" distancia: "+errorCometido());
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
//        mundo = assetManager.loadModel("Scenes/escena.j3o");
//        mundo.setName("tierra");
//        mundo.setLocalTranslation(0f,-1f, 0);
//        rootNode.attachChild(mundo);
//        fisicaSuelo = new RigidBodyControl(0.0f);
//        mundo.addControl(fisicaSuelo);
//        estadosFisicos.getPhysicsSpace().add(fisicaSuelo);
//        fisicaSuelo.setRestitution(0.9f);
        
        //Cielo
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        
        //Ajusta luces
        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(-4f, -5f, 4f).normalizeLocal());
        light.setColor(ColorRGBA.White);
        rootNode.addLight(light);
        
        //Carga area
        area = assetManager.loadModel("Models/area.j3o");
        area.setName("suelo");
        area.scale(3,1,3);
        rootNode.attachChild(area);
        area.setLocalTranslation(new Vector3f(0, 0, 0));
        fisicaArea = new RigidBodyControl(0.0f);
        area.addControl(fisicaArea);
        estadosFisicos.getPhysicsSpace().add(fisicaArea);
        fisicaArea.setRestitution(0.9f);
        
        //Canasta
        canasta = assetManager.loadModel("Models/canasta.j3o");
        canasta.setName("canasta");
        rootNode.attachChild(canasta);
        canasta.setLocalTranslation(new Vector3f(0, 0.5f, -10));
        fisicaCanasta = new RigidBodyControl(0f);
        canasta.addControl(fisicaCanasta);
        estadosFisicos.getPhysicsSpace().add(fisicaCanasta);
        pCanasta = fisicaCanasta.getPhysicsLocation();
        
        //Colisiones
        estadosFisicos.getPhysicsSpace().addCollisionListener(physicsCollisionListener);
        
        //Estado incial del juego
        lanzando = false;
        finLanzamiento = true;
        
        //Aprendizaje
        try {
            String datos = System.getProperty("user.dir") + "/disparos.arff";
            casosEntrenamiento = new Instances(new BufferedReader(new FileReader(datos)));
            casosEntrenamiento.setClassIndex(casosEntrenamiento.numAttributes() - 1); //TODO: PROBAR
            conocimiento = new M5P();
        } catch (IOException e) {
            //Ignore
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if(finLanzamiento && !lanzando) {
            try {
                //Qué error tiene el conocimiento aprendido hasta ahora?
                conocimiento.buildClassifier(casosEntrenamiento);
                Evaluation evaluador = new Evaluation(casosEntrenamiento);
                evaluador.crossValidateModel(conocimiento, casosEntrenamiento, 5, new Debug.Random(1));
                double errorPromedio = evaluador.meanAbsoluteError();

                casoAdecidir = new Instance(casosEntrenamiento.numAttributes());
                casoAdecidir.setDataset(casosEntrenamiento);
            
                float fuerza = (float)Math.random() * 5;
                float angulo = 45; //TODO: TEMPORAL
                casoAdecidir.setValue(0, 20); //TODO: POSICIÓN ACTUAL, TEMPORAL
                //casoAdecidir.setValue(1, fuerza); //TODO: POSICIÓN ACTUAL, TEMPORAL
                //casoAdecidir.setValue(2, angulo); //TODO: POSICIÓN ACTUAL, TEMPORAL

                //si el conocimiento aprendido obtuvo una puntuación satisfactoria usarlo...
                float valorPredicho = fuerza;
                if (errorPromedio < 6) {
                    valorPredicho = (float) conocimiento.classifyInstance(casoAdecidir);
                    
                } else {
                    System.out.println("ERROR ALTO: " + errorPromedio);
                }

                finLanzamiento = false;
                nuevaPelota();
                lanzando = true;
                disparaPelota(valorPredicho, pCanasta, angulo);
                
            } catch (Exception ex) {
                //Ignore
            }
        }
        
        if(pPelota != null)
            fisicaPelota.getPhysicsLocation();
        if(pCanasta != null)
            pCanasta = fisicaCanasta.getPhysicsLocation();
    }
    
    private double distanciaObjetivo() {
        return Math.sqrt(Math.pow(pPelota.x - pCanasta.x, 2) + Math.pow(pPelota.z - pCanasta.z, 2));
    }
    
    private double errorCometido() {
        
        return fisicaPelota.getPhysicsLocation().x - fisicaCanasta.getPhysicsLocation().x + fisicaPelota.getPhysicsLocation().z - fisicaCanasta.getPhysicsLocation().z;
    }
    
    private void disparaPelota(float fuerza, Vector3f direccion, float angulo) {
        float x = (pPelota.x - direccion.x);
        float z = (pPelota.z - direccion.z);
        x = -x / Math.max(x, z);
        z = -z / Math.max(x, z);
        
        if(angulo > 45) {
            x -= (angulo*x)/90;
            z -= (angulo*z)/90;
            angulo = 1f;
        } else {
            angulo /= 45;
        }
        
        Vector3f impulso = new Vector3f(x, angulo, z);
        fisicaPelota.applyImpulse(impulso.mult(fuerza), new Vector3f(0, 0, 0));
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    private void nuevaPelota() {
        if (pelota != null && fisicaPelota != null) {
            rootNode.detachChildNamed("pelota");
            estadosFisicos.getPhysicsSpace().remove(fisicaPelota);
        }
        
        Sphere sphere2 = new Sphere(32, 32, 0.4f);
        TangentBinormalGenerator.generate(sphere2);
        Material mat_bola2 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_bola2.setTexture("DiffuseMap", assetManager.loadTexture("Textures/dirt.jpg"));
        mat_bola2.setBoolean("UseMaterialColors",true);
        mat_bola2.setColor("Diffuse",ColorRGBA.Orange);
        mat_bola2.setColor("Specular",ColorRGBA.Orange);
        mat_bola2.setFloat("Shininess", 640f);
        pelota = new Geometry("pelota", sphere2);
        pelota.setMaterial(mat_bola2);
        pelota.setLocalTranslation(new Vector3f(0, 1f, 10));
        rootNode.attachChild(pelota);
        
        fisicaPelota = new RigidBodyControl(1f);
        pelota.addControl(fisicaPelota);
        estadosFisicos.getPhysicsSpace().add(fisicaPelota);
        fisicaPelota.setRestitution(0.6f);
        fisicaPelota.setMass(0.5f);
        pPelota = fisicaPelota.getPhysicsLocation();
    }
}
