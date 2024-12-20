/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.fuseki.build.model;

import static java.lang.String.format;
import static org.apache.jena.fuseki.build.model.BuildLibM.getZeroOrOne;
import static org.apache.jena.fuseki.build.model.BuildLibM.nodeLabel;
import static org.apache.jena.fuseki.server.FusekiVocab.*;

import java.lang.reflect.Method;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.assembler.Assembler;
import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.FusekiConfigException;
import org.apache.jena.fuseki.FusekiException;
import org.apache.jena.fuseki.auth.AuthPolicy;
import org.apache.jena.fuseki.auth.AuthPolicyList;
import org.apache.jena.fuseki.build.FusekiConfig;
import org.apache.jena.fuseki.server.*;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;
import org.apache.jena.sparql.core.assembler.NamedDatasetAssembler;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.sparql.util.graph.GraphUtils;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;

/** Functions to setup and act on the configuration of a Fuseki server */
//public
class FusekiConfigM {
    private static Logger log = Fuseki.configLog;

    // The default setup of a DataService.
    private static Map<String, Operation> stdRead =
            Map.of("sparql",   Operation.Query,
                   "query",    Operation.Query,
                   "data",     Operation.GSP_R,
                   "get",      Operation.GSP_R);

    private static Map<String, Operation> stdWrite =
            Map.of("sparql",   Operation.Query,
                   "query",    Operation.Query,
                   "update",   Operation.Update,
                   "data",     Operation.GSP_RW,
                   "get",      Operation.GSP_R,
                   "patch",    Operation.Patch);

    private static Set<Operation> stdDatasetRead =
            Set.of(Operation.Query,
                   Operation.GSP_R);

    private static Set<Operation> stdDatasetWrite =
            Set.of(Operation.Query,
                   Operation.Update,
                   Operation.GSP_RW,
                   Operation.Patch);

    static { Fuseki.init(); }

    /** Convenience operation to populate a {@link DataService} with the conventional default services. */
    public static DataService.Builder populateStdServices(DataService.Builder dataServiceBuilder, boolean allowUpdate) {
        return FusekiConfig.populateStdServices(dataServiceBuilder, allowUpdate);

//        Set<Endpoint> endpoints = new HashSet<>();
//        if ( allowUpdate ) {
//            stdWrite.forEach((name, op) -> accEndpoint(endpoints, op, name));
//            stdDatasetWrite.forEach(op -> accEndpoint(endpoints, op));
//        } else {
//            stdRead.forEach((name, op) -> accEndpoint(endpoints, op, name));
//            stdDatasetRead.forEach(op -> accEndpoint(endpoints, op));
//            if ( FusekiExt.extraOperationServicesRead != null )
//                FusekiExt.extraOperationServicesRead.forEach((name, op) -> accEndpoint(endpoints, op, name));
//        }
//        endpoints.forEach(dataServiceBuilder::addEndpoint);
//        return dataServiceBuilder;
    }

//    private static void addDataService(DataAccessPointRegistry dataAccessPoints, String name, DataService dataService) {
//
//        name = DataAccessPoint.canonical(name);
//        if ( dataAccessPoints.isRegistered(name) )
//            throw new FusekiConfigException("Data service name already registered: "+name);
//        DataAccessPoint dap = new DataAccessPoint(name, dataService);
//        dataAccessPoints.register(dap);
//    }
//
//    public static void addDataset(DataAccessPointRegistry dataAccessPoints, String name, DatasetGraph dsg, boolean withUpdate) {
//        name = DataAccessPoint.canonical(name);
//        if ( dataAccessPoints.isRegistered(name) )
//            throw new FusekiConfigException("Data service name already registered: "+name);
//        DataService dataService = buildDataServiceStd(dsg, withUpdate);
//        DataAccessPoint dap = new DataAccessPoint(name, dataService);
//        dataAccessPoints.register(dap);
//    }

    public static DataService buildDataServiceStd(DatasetGraph dsg, boolean withUpdate) {
        return FusekiConfig.buildDataServiceStd(dsg, withUpdate);
//        return DataService.newBuilder(dsg)
//                .withStdServices(withUpdate)
//                .build();
    }

    public static void removeDataset(DataAccessPointRegistry dataAccessPoints, String name) {
        FusekiConfig.removeDataset(dataAccessPoints, name);
//        name = DataAccessPoint.canonical(name);
//        dataAccessPoints.remove(name);
    }

    /** Get the allowed users on a resource.
     *  Returns null if the resource is null or if there were no settings.
     *
     * @return RequestAuthorization
     */
    public static AuthPolicy allowedUsers(Resource resource) {
        return FusekiConfig.allowedUsers(resource.getModel().getGraph(), resource.asNode());
//        if ( resource == null )
//            return null;
//        Collection<RDFNode> allowedUsers = BuildLib.getAll(resource, "fu:"+pAllowedUsers.getLocalName());
//        if ( allowedUsers == null )
//            // Indicate no settings.
//            return null;
//        // Check all values are simple strings
//        List<String> bad = allowedUsers.stream()
//            .map(RDFNode::asNode)
//            .filter(rn -> ! Util.isSimpleString(rn))
//            .map(rn->rn.toString())
//            .collect(toList());
//        if ( ! bad.isEmpty() ) {
//            //Fuseki.configLog.error(format("User names must be a simple string: bad = %s", bad));
//            throw new FusekiConfigException(format("User names should be a simple string: bad = %s", bad));
//        }
//        // RDFNodes/literals to strings.
//        Collection<String> userNames = allowedUsers.stream()
//            .map(RDFNode::asNode)
//            .map(Node::getLiteralLexicalForm)
//            .collect(toList());
//        return Auth.policyAllowSpecific(userNames);
    }

    /**
     * Process a configuration file and return the {@link DataAccessPoint DataAccessPoints};
     * set the context provided for server-wide settings.
     *
     * This bundles together the steps:
     * <ul>
     * <li>{@link #findServer}
     * <li>{@link #parseContext}
     * <li>{@link #processLoadClass} (legacy)
     * <li>{@link #servicesAndDatasets}
     * </ul>
     */
    public static List<DataAccessPoint> processServerConfiguration(Model configuration, Context context) {
        return FusekiConfig.processServerConfiguration(configuration.getGraph(), context);
//        Resource server = findServer(configuration);
//        if ( server != null ) {
//            mergeContext(server, context);
//            processLoadClass(server);
//        }
//        // Process services, whether via server ja:services or, if absent, by finding by type.
//        return servicesAndDatasets$(server, configuration);
    }

    /* Find the server resource in a configuration file.
     * Returns null if there isn't one.
     * Raises {@link FusekiConfigException} is there are more than one.
     */
    public static Resource findServer(Model model) {
        Node n = FusekiConfig.findServer(model.getGraph());
        if ( n == null )
            return null;
        return resourceAdapter(model.getGraph(), n);

//        List<Resource> servers = GraphUtils.listResourcesByType(model, FusekiVocab.tServer);
//        if ( servers.size() == 0 )
//            // "No server" is fine.
//            return null;
//        if ( servers.size() > 1 )
//            throw new FusekiConfigException(servers.size()
//                                            + " servers found (must be exactly one in a configuration file)");
//        // ---- Server
//        Resource server = servers.get(0);
//        return server;
    }

    private static Resource resourceAdapter(Graph graph, Node node) {
        Model m = ModelFactory.createModelForGraph(graph);
        RDFNode rNode = m.asRDFNode(node);
        if ( ! rNode.isResource() )
            throw new FusekiConfigException("Not a resource: "+node);
        return rNode.asResource();
    }

    /**
     * Process the resource for {@link Context} settings.
     * Return a new {@link Context}
     */
    private static Context parseContext(Resource resource) {
        if ( resource == null )
            return null;
        return AssemblerUtils.parseContext(resource);
    }

    /**
     * Process the resource for {@link Context} settings
     * and update an existing {@link Context}.
     */
    private static void mergeContext(Resource resource, Context context) {
        if ( resource == null )
            return ;
        AssemblerUtils.mergeContext(resource, context);
    }

    /**
     * Process any {@code ja:loadClass}
     */
    public static void processLoadClass(Resource server) {
        FusekiConfig.processLoadClass(server.getModel().getGraph(), server.asNode());
//        if ( server == null )
//            return;
//        StmtIterator sIter = server.listProperties(JA.loadClass);
//        for (; sIter.hasNext(); ) {
//            Statement s = sIter.nextStatement();
//            RDFNode rn = s.getObject();
//            String className = null;
//            if ( rn instanceof Resource res ) {
//                String uri = res.getURI();
//                if ( uri == null ) {
//                    log.warn("Blank node for class to load");
//                    continue;
//                }
//                String javaScheme = "java:";
//                if ( !uri.startsWith(javaScheme) ) {
//                    log.warn("Class to load is not 'java:': " + uri);
//                    continue;
//                }
//                className = uri.substring(javaScheme.length());
//            }
//            if ( rn instanceof Literal lit)
//                className = lit.getLexicalForm();
//            loadAndInit(className);
//        }
    }

    /** Find and process datasets and services in a configuration file.
     * This can be a Fuseki server configuration file or a services-only configuration file.
     * It looks {@code fuseki:services ( .... )} then, if not found, all {@code rtdf:type fuseki:services}.
     * @see #processServerConfiguration
     */
    public static List<DataAccessPoint> servicesAndDatasets(Model model) {
        Resource server = findServer(model);
        return servicesAndDatasets$(server, model);
    }

    private static List<DataAccessPoint> servicesAndDatasets$(Resource server, Model model) {
        DatasetDescriptionMapM dsDescMap = new DatasetDescriptionMapM();
        NamedDatasetAssembler.sharedDatasetPool.clear();
        // ---- Services
        // Server to services.
        ResultSet rs = BuildLibM.query("SELECT * { ?s fu:services [ list:member ?service ] }", model, "s", server);
        List<DataAccessPoint> accessPoints = new ArrayList<>();

        // If none, look for services by type.
        if ( ! rs.hasNext() )
            // No "fu:services ( .... )" so try looking for services directly.
            // This means Fuseki2, service configuration files (no server section) work for --conf.
            rs = BuildLibM.query("SELECT ?service { ?service a fu:Service }", model);

        // rs is a result set of services to process.
        for (; rs.hasNext(); ) {
            QuerySolution soln = rs.next();
            Resource svc = soln.getResource("service");
            DataAccessPoint acc = buildDataAccessPoint(svc, dsDescMap);
            if ( acc != null )
                accessPoints.add(acc);
        }
        return accessPoints;
    }

    private static void loadAndInit(String className) {
        try {
            Class<? > classObj = Class.forName(className);
            log.info("Loaded " + className);
            Method initMethod = classObj.getMethod("init");
            initMethod.invoke(null);
        }
        catch (ClassNotFoundException ex) {
            log.warn("Class not found: " + className);
        }
        catch (Exception e) {
            throw new FusekiConfigException(e);
        }
    }

    private static Model readAssemblerFile(String filename) {
        return AssemblerUtils.readAssemblerFile(filename);
    }

    // ---- Directory of assemblers

    /** Read service descriptions in the given directory */
    public static List<DataAccessPoint> readConfigurationDirectory(String dir) {
        return FusekiConfig.readConfigurationDirectory(dir);
//        Path pDir = Path.of(dir).normalize();
//        File dirFile = pDir.toFile();
//        if ( ! dirFile.exists() ) {
//            log.warn("Not found: directory for assembler files for services: '"+dir+"'");
//            return Collections.emptyList();
//        }
//        if ( ! dirFile.isDirectory() ) {
//            log.warn("Not a directory: '"+dir+"'");
//            return Collections.emptyList();
//        }
//        // Files that are not hidden.
//        DirectoryStream.Filter<Path> filter = (entry)-> {
//            File f = entry.toFile();
//            final Lang lang = filenameToLang(f.getName());
//            return ! f.isHidden() && f.isFile() && lang != null && isRegistered(lang);
//        };
//
//        List<DataAccessPoint> dataServiceRef = new ArrayList<>();
//        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pDir, filter)) {
//            for ( Path p : stream ) {
//                DatasetDescriptionMap dsDescMap = new DatasetDescriptionMap();
//                String fn = IRILib.filenameToIRI(p.toString());
//                log.info("Load configuration: "+fn);
//                Model m = readAssemblerFile(fn);
//                readConfiguration(m, dsDescMap, dataServiceRef);
//            }
//        } catch (IOException ex) {
//            log.warn("IOException:"+ex.getMessage(), ex);
//        }
//        return dataServiceRef;
    }

    /** Read a configuration in a model.
     * Allow dataset descriptions to be carried over from another place.
     * Add to a list.
     */
    private static void readConfiguration(Model m, DatasetDescriptionMapM dsDescMap, List<DataAccessPoint> dataServiceRef) {
        List<Resource> services = GraphUtils.listResourcesByType(m, FusekiVocab.fusekiService);

        if ( services.size() == 0 ) {
            log.error("No services found");
            throw new FusekiConfigException();
        }

        for ( Resource service : services ) {
            DataAccessPoint acc = buildDataAccessPoint(service, dsDescMap);
            if ( acc != null )
                dataServiceRef.add(acc);
        }
    }

    /** Build a DataAccessPoint, including DataService, from the description at Resource svc */
    public static DataAccessPoint buildDataAccessPoint(Resource svc, DatasetDescriptionMapM dsDescMap) {
        RDFNode n = BuildLibM.getOne(svc, FusekiVocab.pServiceName);
        try {
            if ( ! n.isLiteral() )
                throw new FusekiConfigException("Not a literal for access point name: "+FmtUtils.stringForRDFNode(n));
            Literal object = n.asLiteral();

            if ( object.getDatatype() != null && ! object.getDatatype().equals(XSDDatatype.XSDstring) )
                Fuseki.configLog.error(format("Service name '%s' is not a string", FmtUtils.stringForRDFNode(object)));

            String name = object.getLexicalForm();
            name = DataAccessPoint.canonical(name);
            AuthPolicy allowedUsers = allowedUsers(svc);
            DataService dataService = buildDataService(svc, dsDescMap).setAuthPolicy(allowedUsers).build();
            DataAccessPoint dataAccess = new DataAccessPoint(name, dataService);
            return dataAccess;
        } catch (FusekiException ex) {
            Fuseki.configLog.error("Skipping: Failed to build service for "+FmtUtils.stringForRDFNode(n));
            Fuseki.configLog.error("    "+ex.getMessage());
            return null;
        }
    }

    private static DataService.Builder buildDataService(Resource fusekiService, DatasetDescriptionMapM dsDescMap) {
        Resource datasetDesc = (Resource)BuildLibM.getOne(fusekiService, FusekiVocab.pDataset);
        Dataset ds = getDataset(datasetDesc, dsDescMap);
        DataService.Builder dataService = DataService.newBuilder(ds.asDatasetGraph());
        Set<Endpoint> endpoints1 = new HashSet<>();
        Set<Endpoint> endpoints2 = new HashSet<>();

        // Old style.
        //    fuseki:serviceQuery "sparql";
        //or
        //    fuseki:serviceQuery [ fuseki:name "sparql" ; fuseki:allowedUsers (..) ];
        accEndpointOldStyle(endpoints1, Operation.Query,    fusekiService,  pServiceQueryEP);
        accEndpointOldStyle(endpoints1, Operation.Update,   fusekiService,  pServiceUpdateEP);
        //accEndpointOldStyle(endpoints1, Operation.Upload,   fusekiService,  pServiceUploadEP);
        accEndpointOldStyle(endpoints1, Operation.GSP_R,    fusekiService,  pServiceReadGraphStoreEP);
        accEndpointOldStyle(endpoints1, Operation.GSP_RW,   fusekiService,  pServiceReadWriteGraphStoreEP);

        // ---- Legacy for old style: a request would also try the dataset (i.e. no endpoint name).
        // If "sparql" then allow /dataset?query=
        // Instead, for old style declarations, add new endpoints to put on the dataset
        // Only complication is that authorization is the AND (all say "yes") of named service authorization.
        {
            Collection<Endpoint> endpointsCompat = oldStyleCompat(dataService, endpoints1);
            endpointsCompat.forEach(dataService::addEndpoint);
        }
        // Explicit definition overrides implied by legacy compatibility.
        // Should not happen.
        endpoints1.forEach(dataService::addEndpoint);

        // New (2019) style -- preferred
        //   fuseki:endpoint [ fuseki:operation fuseki:query ; fuseki:name "" ; fuseki:allowedUsers (....) ] ;
        //   and more.

        accFusekiEndpoints(endpoints2, fusekiService, dsDescMap);
        // This will overwrite old style entries of the same fuseki:name.
        endpoints2.forEach(dataService::addEndpoint);

        return dataService;
    }

    /**
     *  Old style compatibility.
     *  For each endpoint in "endpoints1", ensure there is an endpoint on the dataset (endpoint name "") itself.
     *  Combine the authentication as "AND" of named endpoints authentication.
     */
    private static Collection<Endpoint> oldStyleCompat(DataService.Builder dataService, Set<Endpoint> endpoints1) {
        Map<Operation, Endpoint> endpoints3 = new HashMap<>();
        endpoints1.forEach(ep->{
           Operation operation = ep.getOperation();
           AuthPolicy auth = ep.getAuthPolicy();

           if ( ! StringUtils.isEmpty(ep.getName()) ) {
               if ( endpoints3.containsKey(operation) ) {
                   Endpoint ep1 = endpoints3.get(operation);
                   // Accumulate Authorization.
                   auth = AuthPolicyList.merge(ep1.getAuthPolicy(), auth);
                   Endpoint ep2 = Endpoint.create(ep.getOperation(), "", auth);
                   endpoints3.put(operation, ep2);
               } else {
                   Endpoint ep2 = Endpoint.create(operation, "", auth);
                   endpoints3.put(operation, ep2);
               }
           }
        });
        // Now, after making all legacy endpoints, remove any that are explicit defined in endpoints1.
        // Given the small numbers involved, it is easier to do it this way than
        // additional logic in the first pass over endpoints1.
        endpoints1.stream()
            .filter(ep->StringUtils.isEmpty(ep.getName()))
            .forEach(ep->endpoints3.remove(ep.getOperation()));
        return endpoints3.values();
    }

    /** Find and parse {@code fuseki:endpoint} descriptions. */
    private
    static void accFusekiEndpoints(Set<Endpoint> endpoints, Resource fusekiService, DatasetDescriptionMapM dsDescMap) {
        StmtIterator endpointsDesc = fusekiService.listProperties(pEndpoint);
        endpointsDesc.forEachRemaining(ep-> {
            if ( ! ep.getObject().isResource() )
                throw new FusekiConfigException("Literal for fuseki:endpoint: expected blank node or resource: "+FmtUtils.stringForRDFNode(fusekiService));
            Endpoint endpoint = buildEndpoint(fusekiService, ep.getObject().asResource());
            endpoints.add(endpoint);
        });
    }

    /** Parse {@code fuseki:endpoint}
     * <pre>
     * fuseki:endpoint [
     *     fuseki:operation fuseki:Query ;
     *     fuseki:opImplementation <java:package.Class>
     *     fuseki:allowedUsers (....) ;
     *
     *     ja:context [ ja:cxtName "arq:queryTimeout" ;  ja:cxtValue "1000" ] ;
     *     ja:context [ ja:cxtName "arq:queryLimit" ;  ja:cxtValue "10000" ] ;
     *     ja:context [ ja:cxtName "tdb:defaultUnionGraph" ;  ja:cxtValue "true" ] ;
     *
     *     and specials:
     *         fuseki:timeout "1000,1000" ;
     *         fuseki:queryLimit 1000;
     *         arq:unionGraph true;
     *     ] ;
     * </pre>
     */
    private static Endpoint buildEndpoint(Resource fusekiService, Resource endpoint) {
        // Endpoints are often blank nodes so use fusekiService in error messages.
        // fuseki:operation
        RDFNode opResource = getZeroOrOne(endpoint, pOperation);
        Operation op = null;
        if ( opResource != null ) {
            if ( ! opResource.isResource() || opResource.isAnon() )
                throw exception("Blank node endpoint operation in service %s", nodeLabel(fusekiService));
            Node opRef = opResource.asNode();
            op = Operation.get(opRef);
        }

        // fuseki:implementation - checking only, not active.
        if ( op == null ) {
            RDFNode rImpl = getZeroOrOne(endpoint, pImplementation);
            if ( rImpl == null )
                throw exception("No implementation for fuseki:operation '%s' in service %s", nodeLabel(opResource), nodeLabel(fusekiService));
            // Global registry. Replace existing registry.
            Pair<Operation, ActionService> x = BuildLibM.loadOperationActionService(rImpl);
            Operation op2 = x.getLeft();
            ActionService proc = x.getRight();
            if ( op2 == null )
                throw exception("Failed to load implementation for fuseki:operation '%s' in service %s", nodeLabel(opResource), nodeLabel(fusekiService));
            op = op2;
            // Using a blank node (!) for the operation means this is safe!
            // OperationRegistry.get().register(op2, proc);
        }

        // fuseki:allowedUsers
        AuthPolicy authPolicy = allowedUsers(endpoint);

        // fuseki:name
        RDFNode epNameR = getZeroOrOne(endpoint, pEndpointName);
        String epName = null;
        if ( epNameR == null ) {
//            // Make required to give "" for dataset, not default to dataset if missing.
//            throw exception("No service name for endpoint", fusekiService, ep, pServiceName);
            epName = Endpoint.DatasetEP.string;
        } else {
            if ( ! epNameR.isLiteral() )
                throw exception("Not a literal for service name for endpoint", fusekiService, endpoint, pEndpointName);
            epName = epNameR.asLiteral().getLexicalForm();
        }

        Context cxt = parseContext(endpoint);

        // Per-endpoint context.
        // Could add special names:
        //   fuseki:timeout
        //   fuseki:queryLimit
        //   fuseki:unionDefaultGraph

        Endpoint ep = Endpoint.create()
            .operation(op)
            // Validates the name.
            .endpointName(epName)
            .authPolicy(authPolicy)
            .context(cxt)
            .build();
        return ep;
    }

    private static FusekiConfigException exception(String fmt, Object...args) {
        String msg = String.format(fmt,  args);
        throw new FusekiConfigException(msg);
    }

    // Old style.
    //    fuseki:serviceQuery "sparql";
    //or
    //    fuseki:serviceQuery [ fuseki:name "sparql" ; fuseki:allowedUsers (..) ];
    private static void accEndpointOldStyle(Collection<Endpoint> endpoints, Operation operation, Resource svc, Property property) {
        String p = "<"+property.getURI()+">";
        ResultSet rs = BuildLibM.query("SELECT * { ?svc " + p + " ?ep}", svc.getModel(), "svc", svc);
        for (; rs.hasNext(); ) {
            QuerySolution soln = rs.next();
            // No policy yet - set below if one is found.
            AuthPolicy authPolicy = null;
            RDFNode ep = soln.get("ep");
            String endpointName = null;
            if ( ep.isLiteral() )
                // fuseki:serviceQuery "sparql"
                endpointName = soln.getLiteral("ep").getLexicalForm();
            else if ( ep.isResource() ) {
                Resource r = (Resource)ep;
                try {
                    // [ fuseki:name ""; fuseki:allowedUsers ( "" "" ) ]
                    Statement stmt = r.getProperty(FusekiVocab.pEndpointName);
                    if ( stmt == null )
                        throw new FusekiConfigException("Expected property <"+FusekiVocab.pEndpointName+"> with <"+property.getURI()+"> for <"+svc+">");
                    endpointName = stmt.getString();
                    List<RDFNode> x = GraphUtils.multiValue(r, FusekiVocab.pAllowedUsers);
                    if ( x.size() > 1 )
                        throw new FusekiConfigException("Multiple fuseki:"+FusekiVocab.pAllowedUsers.getLocalName()+" for "+r);
                    if ( ! x.isEmpty() )
                        authPolicy = allowedUsers(r);
                } catch(JenaException | ClassCastException ex) {
                    throw new FusekiConfigException("Failed to parse endpoint: "+r);
                }
            } else {
                throw new FusekiConfigException("Unrecognized: "+ep);
            }

            if ( StringUtils.isEmpty(endpointName) )
                endpointName = null;
            Endpoint endpoint = Endpoint.create(operation, endpointName, authPolicy);
            endpoints.add(endpoint);
        }
    }

    private static void accEndpoint(Collection<Endpoint> endpoints, Operation operation) {
        accEndpoint(endpoints, operation, null);
    }

    private static void accEndpoint(Collection<Endpoint> endpoints, Operation operation, String endpointName) {
        accEndpoint(endpoints, operation, endpointName, null);
    }

    private static void accEndpoint(Collection<Endpoint> endpoints, Operation operation, String endpointName, AuthPolicy authPolicy) {
        if ( StringUtils.isEmpty(endpointName) )
            endpointName = null;
        Endpoint endpoint = Endpoint.create(operation, endpointName, authPolicy);
        endpoints.add(endpoint);
    }

    public static Dataset getDataset(Resource datasetDesc, DatasetDescriptionMapM dsDescMap) {
        // check if this one already built
        // This is absolute and does not require a NamedDatasetAssembler and to have a ja:name.
        // ja:name/NamedDatasetAssembler must be used if the service datasets need to
        // wire up sharing of a graph of datasets (not TDB).

        Dataset ds = dsDescMap.get(datasetDesc);
        if ( ds != null )
            return ds;

        // Not seen before.
        // Check if the description is in the model.
        if ( !datasetDesc.hasProperty(RDF.type) )
            throw new FusekiConfigException("No rdf:type for dataset " + nodeLabel(datasetDesc));

        // Should have been done already. e.g. ActionDatasets.execPostContainer,
        // AssemblerUtils.readAssemblerFile < FusekiServer.parseConfigFile.
        //AssemblerUtils.addRegistered(datasetDesc.getModel());
        ds = (Dataset)Assembler.general.open(datasetDesc);
        dsDescMap.register(datasetDesc, ds);
        return ds;
    }
}
