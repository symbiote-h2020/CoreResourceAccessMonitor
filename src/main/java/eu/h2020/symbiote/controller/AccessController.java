package eu.h2020.symbiote.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.SensorRepository;
import eu.h2020.symbiote.model.Sensor;
import eu.h2020.symbiote.model.Platform;

@CrossOrigin
@RestController
@RequestMapping("/cram_api")
public class AccessController {

    private static Log log = LogFactory.getLog(AccessController.class);

    @Autowired
    private PlatformRepository platformRepo;

    @Autowired
    private SensorRepository sensorRepo;

    @RequestMapping(value="/resource_urls/{resourceIdList}", method=RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Map<String, String>> accessResources(@PathVariable String[] resourceIdList) throws MalformedURLException {

        Map<String, String> ids = new HashMap();

        for(String id : resourceIdList) {
            Sensor sensor = sensorRepo.findOne(id);
            if (sensor != null)
            {
                URL url = sensor.getResourceURL();
                ids.put(sensor.getId(), url.toString());
                log.info("AccessController found a resource with id " + sensor.getId() +
                     " and url " + url.toString());
            }

        }

        return new ResponseEntity<Map<String, String>> (ids, HttpStatus.OK);
    }

    @RequestMapping(value="/resource_urls", method=RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Map<String, String>> accessResources(@RequestBody JSONObject resourceIdList) throws MalformedURLException {

        Map<String, String> ids = new HashMap();

        ArrayList<String> array = (ArrayList<String>)resourceIdList.get("idList");

        Iterator<String> iterator = array.iterator();
        while (iterator.hasNext()) {
            Sensor sensor = sensorRepo.findOne(iterator.next());
            if (sensor != null)
            {
                URL url = sensor.getResourceURL();
                ids.put(sensor.getId(), url.toString());
                log.info("AccessController found a resource with id " + sensor.getId() +
                     " and url " + url.toString());
            }
        }

        return new ResponseEntity<Map<String, String>> (ids, HttpStatus.OK);
    }
}

