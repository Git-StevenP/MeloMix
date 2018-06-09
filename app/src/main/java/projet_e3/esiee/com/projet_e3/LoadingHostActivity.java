package projet_e3.esiee.com.projet_e3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.jr.ob.JSON;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import weka.associations.BinaryItem;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToBinary;
import weka.filters.unsupervised.attribute.Remove;

public class LoadingHostActivity extends AppCompatActivity {
    private ListView listView;
    private TextView TxtStatus;
    private Button NextBtn;
    private WifiP2pManager aManager;
    private WifiP2pManager.Channel aChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntent;

    private List<WifiP2pDevice> peers = new ArrayList<>();
    private ArrayAdapter<String> hAdapter;
    private String[] deviceName;
    private WifiP2pDevice[] deviceArray;
    private final WifiP2pConfig config = new WifiP2pConfig();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_host);
        analyseData();
        work();
        exqWork();
    }

    private void analyseData(){
        File rootDataDir = this.getFilesDir();
        FilenameFilter jsonFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".json");
            }
        };
        File[] files = rootDataDir.listFiles(jsonFilter);

        int numberOfUsers = files.length;
        ArrayList<Attribute> attributeArrayList = new ArrayList<Attribute>();

        for (int i = 0; i < numberOfUsers; i++) {
            Log.d("Files", "FileName:" + files[i].getName());
            Map<Object,Object> map = null;
            try {
                map = JSON.std.mapFrom(files[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Log.i("mapKeyset", map.keySet().toString());
            //Log.i("mapValues", map.values().toString());
            for(Map.Entry<Object, Object> entry : map.entrySet()){
                Attribute genre = new Attribute((String)entry.getKey());
                //genre.setWeight((double)map.get(genre));
                /*Log.i("genre", (String)entry.getKey());
                Log.i("getGenre", "" + entry.getValue());*/

                if(!attributeArrayList.contains(genre)){
                    //genre.setWeight((double) entry.getValue());
                    attributeArrayList.add(genre);
                }/*else{
                    Attribute currentGenreInList = attributeArrayList.get(attributeArrayList.indexOf(genre));
                    currentGenreInList.setWeight(currentGenreInList.weight() + (double) entry.getValue());
                    //Log.i("AttributeWeight", "" + currentGenreInList.weight());
                }*/
            }
        }

        //Log.i("ListOfAttributes", attributeArrayList.toString());
        int numberOfGenres = attributeArrayList.size();
        Instances dataBase = new Instances("usersGenres", attributeArrayList, numberOfUsers);

        for (int i = 0; i < numberOfUsers; i++) {
            Map<Object,Object> map = null;
            try {
                map = JSON.std.mapFrom(files[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Instance user = new SparseInstance(numberOfGenres);
            for(int p = 0; p<attributeArrayList.size(); p++){
                Attribute currentAttribute = attributeArrayList.get(p);
                String attributeName = currentAttribute.name();
                if(map.containsKey(attributeName)){
                    if(map.get(attributeName) != null)
                        user.setValue(currentAttribute, (double) map.get(attributeName));
                }/*else
                    user.setValue(currentAttribute, 0);*/
            }
            dataBase.add(user);
        }

        SimpleKMeans simpleKMeans = new SimpleKMeans();
        try {
            if(numberOfUsers == 2)
                simpleKMeans.setNumClusters(2);
            else
                simpleKMeans.setNumClusters((int)Math.round(0.33*numberOfUsers+0.66));

            simpleKMeans.setMaxIterations(10);
            simpleKMeans.setPreserveInstancesOrder(true);
            simpleKMeans.buildClusterer(dataBase);
            int[] assignments = simpleKMeans.getAssignments();
            int i = 0;
            for(int clusterNum : assignments) {
                Log.i("Instance " + i, " -> Cluster " + clusterNum + "\n");
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Instances centroidsBase = simpleKMeans.getClusterCentroids();
        int centroidsBaseSize = centroidsBase.size();
        Remove removeFilter = new Remove();
        ArrayList<Integer> indexOfAttributeToKeep = new ArrayList<Integer>();
        for(int i=0; i<centroidsBaseSize; i++){
            Instance currentInstance = new SparseInstance(centroidsBase.get(i));
            //Log.i("instanceAvant", currentInstance.toString());
            int numberAttributes = currentInstance.numAttributes();
            HashMap<Integer, Double> attributeHashMap = new HashMap<>();
            for(int p = 0; p<numberAttributes; p++){
                attributeHashMap.put(p, currentInstance.value(p));
            }
            LinkedHashMap<Integer, Double> sortedHashMap = sortHashMapByValues(attributeHashMap);
            Iterator<Integer> hashIterator = sortedHashMap.keySet().iterator();
            for(int j = 0;hashIterator.hasNext() && j < Math.round(sortedHashMap.size()*0.10); j++){
                Integer currentInt = hashIterator.next();
                if(!indexOfAttributeToKeep.contains(currentInt))
                    indexOfAttributeToKeep.add(currentInt);
            }
            //Log.i("instanceApres", currentInstance.toString());
        }

        int[] indexOfAttributeToKeepArray = convertIntegers(indexOfAttributeToKeep);
        removeFilter.setAttributeIndicesArray(indexOfAttributeToKeepArray);
        removeFilter.setInvertSelection(true);
        Instances centroidsBaseSimplified = null;
        try {
            removeFilter.setInputFormat(centroidsBase);
            centroidsBaseSimplified = Filter.useFilter(centroidsBase, removeFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i("centroidsBaseSimplified", centroidsBaseSimplified.toSummaryString());

        /*NumericToBinary filter = new NumericToBinary();
        //Data.setClassIndex(-1);

        try {
            filter.setInputFormat(dataBase);
            //dataBase = Filter.useFilter(dataBase, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        //Log.i("dataBaseSummary", dataBase.toSummaryString());
        //Log.i("dataBase", dataBase.toString());

        /*FPGrowth algo = new FPGrowth();
        algo.setLowerBoundMinSupport(0.9); //on définit le seuil minimum
        algo.setNumRulesToFind(0);
        try {
            //algo.buildAssociations(dataBase);
            algo.getFrequentItems(dataBase);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int sizeItemsets = algo.m_largeItemSets.size();
        Iterator<FPGrowth.FrequentBinaryItemSet> itemsets = algo.m_largeItemSets.iterator();
        Log.i("frequentItemsetsSize", "" + sizeItemsets);
        while(itemsets.hasNext()){
            Log.i("itemset", itemsets.next().toString());
        }*/
    }

    private int[] convertIntegers(ArrayList<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = integers.get(i);
        }
        return ret;
    }

    private LinkedHashMap<Integer, Double> sortHashMapByValues(
            HashMap<Integer, Double> passedMap) {
        List<Integer> mapKeys = new ArrayList<>(passedMap.keySet());
        List<Double> mapValues = new ArrayList<>(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);
        Collections.reverse(mapValues);
        Collections.reverse(mapKeys);

        LinkedHashMap<Integer, Double> sortedMap =
                new LinkedHashMap<>();

        Iterator<Double> valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Double val = valueIt.next();
            Iterator<Integer> keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                Integer key = keyIt.next();
                Double comp1 = passedMap.get(key);
                Double comp2 = val;

                if (comp1.equals(comp2)) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }
        return sortedMap;
    }

    private void work() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert wifiManager != null;
        if(!wifiManager.isWifiEnabled()){
            wifiManager.setWifiEnabled(true);
        }
        aManager = (WifiP2pManager) getApplicationContext().getSystemService(Context.WIFI_P2P_SERVICE);
        aChannel = aManager.initialize(this,getMainLooper(),null);

        deletePersistentGroup();
        //disconnect();

        config.groupOwnerIntent = 15;

        mReceiver = new BroadCast(aManager,aChannel,null,this, wifiManager);
        mIntent = new IntentFilter();
        setAction();

        this.createGrp();

        listView = findViewById(R.id.HostList);
        TxtStatus = findViewById(R.id.KieKi);
        NextBtn = findViewById(R.id.NextBtn);
        //this.discover();

    }

    public void setAction(){
        mIntent.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntent.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntent.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntent.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void exqWork() {

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                final WifiP2pDevice device = deviceArray[i];
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                aManager.connect(aChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"connected to "+ device.deviceName, Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(),"Fail connected to "+ device.deviceName, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

        NextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoadingHostActivity.this, HostActivity.class);
                intent.putExtra("authToken", getIntent().getStringExtra("authToken"));
                intent.putExtra("host",1);
                startActivity(intent);
            }
        });

    }

    private void deletePersistentGroup(){
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (Method method : methods) {
                if (method.getName().equals("deletePersistentGroup")) {
                    for (int netid = 0; netid < 32; netid++) {
                        method.invoke(aManager, aChannel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    WifiP2pManager.PeerListListener peerListListener =  new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peersDevice) {
            if (!peersDevice.getDeviceList().equals(peers)){
                peers.clear();
                peers.addAll(peersDevice.getDeviceList());
                deviceName = new String[peersDevice.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peersDevice.getDeviceList().size()];
                int index =0;
                for(WifiP2pDevice device : peersDevice.getDeviceList())
                {
                    //if(!device.isGroupOwner()) {
                    deviceName[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                    //}

                }
                hAdapter = new ArrayAdapter<>(listView.getContext(), android.R.layout.simple_list_item_1, deviceName);
                listView.setAdapter(hAdapter);
            }
            if (peers.isEmpty()) {
                Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_SHORT).show();
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            if(info.groupFormed && info.isGroupOwner){
                TxtStatus.setText("Host");
                HostClass hostClass = new HostClass(getApplicationContext());
                hostClass.start();
            }
        }
    };

    public void discover(){
        aManager.discoverPeers(aChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "Succeed disco", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason){
                Toast.makeText(getApplicationContext(), "Fail disco", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void createGrp(){
        aManager.createGroup(aChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {Toast.makeText(getApplicationContext(), "Succeed create", Toast.LENGTH_SHORT).show(); }

            @Override
            public void onFailure(int i) {Toast.makeText(getApplicationContext(), "fail creat", Toast.LENGTH_SHORT).show(); }
        });
    }

    public void disconnect(){
        aManager.removeGroup(aChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"Remove S",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(getApplicationContext(),"Remove F "+ i,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
        aManager.cancelConnect(aChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {

            }
        });
    }

    public static class FileServerAsyncTask extends AsyncTask<String, String, String> {

        private Context mFilecontext;
        private int PORT;

        FileServerAsyncTask(Context context, int port) {
            this.mFilecontext = context;
            this.PORT = port;
        }


        @Override
        protected String doInBackground(String... params) {
            try {
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));

                Socket client = serverSocket.accept();

                try {
                    Random random = new Random();
                    final File f = new File(mFilecontext.getFilesDir(), String.valueOf(random.nextInt(100))+String.valueOf(System.currentTimeMillis())+String.valueOf(random.nextInt(100))+".json");

                    File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();

                    InputStream inputstream = client.getInputStream();

                    copyFile(inputstream, new FileOutputStream(f));

                    serverSocket.close();
                    return f.getAbsolutePath();

                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {

                Toast.makeText(mFilecontext,"File transmis"+result,Toast.LENGTH_SHORT).show();

                if (!TextUtils.isEmpty(result)) {

                    FileServerAsyncTask FileServerobj = new
                            FileServerAsyncTask(mFilecontext, FileTransferService.PORT);
                    FileServerobj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{null});
                }
            }

        }


        static void copyFile(InputStream inputStream, OutputStream out) {

            byte buf[] = new byte[8500];
            int len;
            try {
                while ((len = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.close();
                inputStream.close();
            } catch (IOException e) {
                e.getMessage();
            }
        }
    }
}
