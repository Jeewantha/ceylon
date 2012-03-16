/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package com.redhat.ceylon.ceylondoc;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Main {
    private static final String CEYLOND_VERSION = "0.2 'Minitel'";
    private static final int SC_OK = 0;
    private static final int SC_ARGS = 1;
    private static final int SC_ERROR = 2;

    public static void main(String[] args) throws IOException {
        String destDir = null;
        List<String> sourceDirs = new LinkedList<String>();
        boolean includeNonShared = false;
        boolean includeSourceCode = false;
        List<String> modules = new LinkedList<String>();
        List<String> repositories = new LinkedList<String>();
        String user = null,pass = null;
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            int argsLeft = arg.length() - 1 - i;
            if ("-h".equals(arg)
                    || "-help".equals(arg)
                    || "--help".equals(arg)) {
                printUsage(SC_OK);
            } else if ("-v".equals(arg)
                        || "-version".equals(arg)
                        || "--version".equals(arg)) {
                printVersion();
            } else if ("-d".equals(arg)) {
                System.err.println("-d: option not yet supported (though perhaps you meant -out?)");
                exit(SC_ARGS);
            } else if ("-out".equals(arg)) {
                if (argsLeft <= 0) {
                    optionMissingArgument(arg);
                }
                destDir = args[++i];
            } else if ("-src".equals(arg)) {
                if (argsLeft <= 0) {
                    optionMissingArgument(arg);
                }
                sourceDirs.addAll(readPath(args[++i]));
            } else if ("-rep".equals(arg)) {
                if (argsLeft <= 0) {
                    optionMissingArgument(arg);
                }
                repositories.add(args[++i]);
            } else if ("-non-shared".equals(arg)) {
                includeNonShared = true;
            } else if ("-source-code".equals(arg)) {
                includeSourceCode = true;
            } else if ("-user".equals(arg)) {
                if (argsLeft <= 0) {
                    optionMissingArgument(arg);
                }
                user = args[++i];
            } else if ("-pass".equals(arg)) {
                if (argsLeft <= 0) {
                    optionMissingArgument(arg);
                }
                pass = args[++i];
            } else if (arg.startsWith("-")) {
                System.err.println(arg+": unknown option");
                exit(SC_ARGS);
            } else {
                modules.add(arg);
            }
            
        }
        
        if(modules.isEmpty()){
            System.err.println("No modules specified");
            printUsage(SC_ARGS);
        }
        if (destDir == null) {
            destDir = "modules";
        }

        List<File> sourceFolders = new LinkedList<File>();
        if (sourceDirs.isEmpty()) {
            File src = new File("source");
            if(src.isDirectory())
                sourceFolders.add(src);
        }else{
            for(String srcDir : sourceDirs){
                File src = new File(srcDir);
                if (!src.isDirectory()) {
                    System.err.println("No such source directory: " + srcDir);
                    exit(SC_ARGS);
                }
                sourceFolders.add(src);
            }
        }

        try{
            CeylonDocTool ceylonDocTool = new CeylonDocTool(sourceFolders, repositories, modules, false);
            ceylonDocTool.setOutputRepository(destDir, user, pass);
            ceylonDocTool.setIncludeNonShared(includeNonShared);
            ceylonDocTool.setIncludeSourceCode(includeSourceCode);
            ceylonDocTool.makeDoc();
        }catch(Exception x){
            System.err.println("Error: "+x.getMessage());
            x.printStackTrace();
            exit(SC_ERROR);
        }
    }

    private static void exit(int statusCode) {
        System.exit(statusCode);
    }
    
    private static void optionMissingArgument(String arg) {
        System.err.println("Expected an argument for " + arg + " option");
        exit(SC_ARGS);
    }

    private static List<String> readPath(String path) {
        List<String> ret = new LinkedList<String>();
        int start = 0;
        int sep;
        while((sep = path.indexOf(File.pathSeparatorChar, start)) != -1){
            String part = path.substring(start, sep);
            if(!part.isEmpty())
                ret.add(part);
            start = sep + 1;
        }
        // rest
        String part = path.substring(start);
        if(!part.isEmpty())
            ret.add(part);
        return ret;
    }

    private static void printVersion() {
        System.out.println("Version: ceylond "+CEYLOND_VERSION);
        exit(SC_OK);
    }

    private static void printUsage(int statusCode) {
        List<String> defaultRepositories = com.redhat.ceylon.compiler.java.util.Util.addDefaultRepositories(Collections.<String>emptyList());
        System.err.print(
                "Usage: ceylond [options...] moduleName[/version]... :\n"
                +"where possible options include:\n"
                +"  -src <directory>:    Path to source files (default: './source')\n"
                +"                       Can be specified multiple times\n"
                +"  -out <url>:          Output module repository (default: './modules')\n"
                +"  -rep <url>:          Module repository\n"
                +"                       Can be specified multiple times\n"
                +"                       Default:\n"
        );
        for(String repo : defaultRepositories)
            System.err.println("                        "+repo);
        System.err.print(
                 "  -non-shared:         Document non-shared declarations\n"
                +"  -source-code:        Include the source code\n"
                +"  -user <value>:       User name for output repository (HTTP only)\n"
                +"  -pass <value>:       Password for output repository (HTTP only)\n"
                +"  -d:                  Not supported yet\n"
                +"  -help:               Prints help usage\n"
                +"  -version:            Prints version number\n"
        );
        exit(statusCode);
    }
}
