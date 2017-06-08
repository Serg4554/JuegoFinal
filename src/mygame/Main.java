package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.M5P;
import weka.core.Debug;
import weka.core.Instance;
import weka.core.Instances;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication {
    private Spatial area;
    private BulletAppState estadosFisicos;
    private RigidBodyControl fisicaArea, fisicaPelota, fisicaCanasta;
    private Geometry pelota;
    private Vector3f pPelota, pCanasta;
    private Spatial canasta;
    private boolean lanzando, finLanzamiento;
    private Classifier conocimiento = null;
    private Instances casosEntrenamiento = null;
    private Instance casoAdecidir;
    private int maximoNumeroCasosEntrenamiento = 300;
    private float fuerza;
    private Vector3f posicionInicialPelota;
    private int numTiro = 0;
    Mesh lineaFuerza, lineaDistancia;
    Geometry lineaFuerzaGeometry, lineaDistanciaGeometry;
    private final int NUMERO_TIROS = 20;
    BitmapText textoFuerza, textoDistancia;
    
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
                if(!finLanzamiento && lanzando){
                    if(numTiro < NUMERO_TIROS) {
                        casoAdecidir = new Instance(casosEntrenamiento.numAttributes());
                        casoAdecidir.setDataset(casosEntrenamiento);
                        casoAdecidir.setValue(0, distaciaAPelota(pPelota));
                        casoAdecidir.setClassValue(fuerza);
                    } else {
                        casoAdecidir.setValue(0, distaciaAPelota(pPelota));
                        casoAdecidir.setClassValue(fuerza);
                    }
                    System.out.println("Caso aprendido. Fuerza: " + fuerza + " Distancia: " + distaciaAPelota(pPelota) + "\n");
                    
                    //Aprende
                    casosEntrenamiento.add(casoAdecidir);
                    for (int i = 0; i < casosEntrenamiento.numInstances() - maximoNumeroCasosEntrenamiento; i++) {
                        casosEntrenamiento.delete(0);  //Hay muchos ejemplos borrar el más antiguo
                    }
                    
                    //Lanzamos de nuevo
                    nuevaPelota();
                    lanzando = false;
                    finLanzamiento = true;
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
        area.scale(8,1,8);
        rootNode.attachChild(area);
        area.setLocalTranslation(new Vector3f(0, 0, 0));
        fisicaArea = new RigidBodyControl(0.0f);
        area.addControl(fisicaArea);
        estadosFisicos.getPhysicsSpace().add(fisicaArea);
        fisicaArea.setRestitution(0.9f);
        
        //Canasta
        canasta = assetManager.loadModel("Models/canasta.j3o");
        canasta.setName("canasta");
        canasta.scale(1.5f, 0.2f, 1.5f);
        rootNode.attachChild(canasta);
        canasta.setLocalTranslation(new Vector3f(0, 0.5f, 0));
        fisicaCanasta = new RigidBodyControl(0f);
        canasta.addControl(fisicaCanasta);
        estadosFisicos.getPhysicsSpace().add(fisicaCanasta);
        pCanasta = fisicaCanasta.getPhysicsLocation();
        
        //Colisiones
        estadosFisicos.getPhysicsSpace().addCollisionListener(physicsCollisionListener);
        
        //Estado incial del juego
        lanzando = false;
        finLanzamiento = true;
        posicionInicialPelota = new Vector3f(0, 1f, 20);
        
        //Lineas
        lineaFuerza = new Mesh();
        lineaFuerza.setMode(Mesh.Mode.Lines);
        lineaFuerzaGeometry = new Geometry("line", lineaFuerza);
        Material lineMaterial = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
        lineaFuerzaGeometry.setMaterial(lineMaterial);
        rootNode.attachChild(lineaFuerzaGeometry);
        lineaDistancia = new Mesh();
        lineaDistancia.setMode(Mesh.Mode.Lines);
        lineaDistanciaGeometry = new Geometry("line", lineaDistancia);
        lineaDistanciaGeometry.setMaterial(lineMaterial);
        rootNode.attachChild(lineaDistanciaGeometry);
        
        //Fuente
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        textoFuerza = new BitmapText(guiFont, false);
        textoFuerza.setSize(0.5f);
        textoFuerza.setColor(ColorRGBA.Yellow);
        textoFuerza.rotate(0, (float)Math.PI * 2.5f, 0);
        textoDistancia = new BitmapText(guiFont, false);
        textoDistancia.setSize(0.5f);
        textoDistancia.setColor(ColorRGBA.Yellow);
        textoDistancia.rotate(0, (float)Math.PI * 2.5f, 0);
        
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
                conocimiento.buildClassifier(casosEntrenamiento);
                Evaluation evaluador = new Evaluation(casosEntrenamiento);
                evaluador.crossValidateModel(conocimiento, casosEntrenamiento, 5, new Debug.Random(1));
                
                System.out.println("Numero de tiro: " + numTiro);
                
                if(numTiro < NUMERO_TIROS) {
                    //Primera fase
                    fuerza = (float)Math.random() * 15 + 1;
                } else {
                    //Segunda fase
                    if(numTiro % 1 == 0) {
                        posicionInicialPelota.x = (float)Math.random() * 20 - 10;
                        posicionInicialPelota.z = (float)Math.random() * 20 - 10;
                    }
                    
                    casoAdecidir = new Instance(casosEntrenamiento.numAttributes());
                    casoAdecidir.setDataset(casosEntrenamiento);
                    casoAdecidir.setValue(0, distaciaACanasta(posicionInicialPelota));
                    fuerza = (float)conocimiento.classifyInstance(casoAdecidir);
                    System.out.println("Caso predicho. Fuerza: " + fuerza + " Distancia: " + distaciaACanasta(posicionInicialPelota) + " (Error: " + evaluador.meanAbsoluteError() + ")");
                }
                numTiro++;

                finLanzamiento = false;
                nuevaPelota();
                lanzando = true;
                disparaPelota(fuerza, pCanasta);
                
            } catch (Exception ex) {
                //Ignore
            }
        }
        
        if(pPelota != null)
            pPelota = fisicaPelota.getPhysicsLocation();
        if(pCanasta != null)
            pCanasta = fisicaCanasta.getPhysicsLocation();
    }
    
    private double distaciaAPelota(Vector3f posicion) {
        return Math.sqrt(Math.pow(posicion.x - posicionInicialPelota.x, 2) + Math.pow(posicion.z - posicionInicialPelota.z, 2));
    }
    
    private double distaciaACanasta(Vector3f posicion) {
        return Math.sqrt(Math.pow(posicion.x - pCanasta.x, 2) + Math.pow(posicion.z - pCanasta.z, 2));
    }
    
    private void disparaPelota(float fuerza, Vector3f direccion) {
        //Calcula impulso
        Vector3f impulso = direccion.subtract(posicionInicialPelota);
        impulso.y = 0;
        impulso = impulso.normalize();
        impulso.y = 1;
        impulso = impulso.normalize();
        impulso = impulso.mult(fuerza);
        
        //Calcula posición final del vector impulso
        Vector3f posImpulso = impulso.clone();
        posImpulso.x += posicionInicialPelota.x;
        posImpulso.y += posicionInicialPelota.y;
        posImpulso.z += posicionInicialPelota.z;
        
        //Dibuja línea fuerza
        lineaFuerza.setBuffer(VertexBuffer.Type.Position, 3, new float[] {
            posicionInicialPelota.x, posicionInicialPelota.y, posicionInicialPelota.z,
            posImpulso.x, posImpulso.y, posImpulso.z
        });
        lineaFuerza.setBuffer(VertexBuffer.Type.Index, 2, new short[]{0, 1});
        lineaFuerza.updateBound();
        lineaFuerza.updateCounts();
        
        //Muestra texto con fuerza
        rootNode.detachChild(textoFuerza);
        textoFuerza.setText("" + Math.round(fuerza*100f)/100f);
        textoFuerza.setLocalTranslation(posImpulso.x, posImpulso.y, posImpulso.z);
        rootNode.attachChild(textoFuerza);
        
        if(numTiro > NUMERO_TIROS) {
            //Dibuja línea distancia
            lineaDistancia.setBuffer(VertexBuffer.Type.Position, 3, new float[] {
                posicionInicialPelota.x, posicionInicialPelota.y, posicionInicialPelota.z,
                pCanasta.x, pCanasta.y + 0.1f, pCanasta.z
            });
            lineaDistancia.setBuffer(VertexBuffer.Type.Index, 2, new short[]{0, 1});
            lineaDistancia.updateBound();
            lineaDistancia.updateCounts();

            //Muestra texto con distancia
            Vector3f posDistancia = posicionInicialPelota.divide(2);
            rootNode.detachChild(textoDistancia);
            textoDistancia.setText(""+Math.round(distaciaACanasta(posicionInicialPelota)*100f)/100f);
            textoDistancia.setLocalTranslation(posDistancia.x, posDistancia.y + 1f, posDistancia.z);
            rootNode.attachChild(textoDistancia);
        }
        
        //Aplica impulso
        fisicaPelota.applyImpulse(impulso, new Vector3f(0, 0, 0));
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
        
        Sphere sphere = new Sphere(32, 32, 0.4f);
        Material mat_bola = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat_bola.setTexture("DiffuseMap", assetManager.loadTexture("Textures/dirt.jpg"));
        mat_bola.setBoolean("UseMaterialColors",true);
        mat_bola.setColor("Diffuse",ColorRGBA.Orange);
        mat_bola.setColor("Specular",ColorRGBA.Orange);
        mat_bola.setFloat("Shininess", 640f);
        pelota = new Geometry("pelota", sphere);
        pelota.setMaterial(mat_bola);
        pelota.setLocalTranslation(posicionInicialPelota);
        rootNode.attachChild(pelota);
        
        fisicaPelota = new RigidBodyControl(1f);
        pelota.addControl(fisicaPelota);
        estadosFisicos.getPhysicsSpace().add(fisicaPelota);
        fisicaPelota.setRestitution(0.6f);
        fisicaPelota.setMass(0.5f);
        pPelota = fisicaPelota.getPhysicsLocation();
    }
}
