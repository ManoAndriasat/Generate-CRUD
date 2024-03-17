package ambovombe.kombarika.generator.service.vueJs;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ambovombe.kombarika.database.DbConnection;
import ambovombe.kombarika.generator.parser.FileUtility;
import ambovombe.kombarika.generator.service.DbService;
import ambovombe.kombarika.generator.utils.ObjectUtility;
import ambovombe.kombarika.utils.Misc;

public class VueJs {
    public static String generateView(String table, String API, String templatePath, DbConnection dbConnection)
            throws Exception {
        String res = "";
        String template = FileUtility.readOneFile(templatePath);
        List<String> primaryKeys = DbService.getPrimaryKey(dbConnection, table);
        String path = ObjectUtility.formatToCamelCase(table);
        HashMap<String, String> columns = DbService.getDetailsColumn(dbConnection.getConnection(), table);
        HashMap<String, String> foreignkeys = DbService.getForeignKeys(dbConnection, table);
        HashMap<String, String> combinedMap = new HashMap<>();

        for (Map.Entry<String, String> entry : columns.entrySet()) {
            if (!foreignkeys.containsKey(entry.getKey())) {
                combinedMap.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : foreignkeys.entrySet()) {
            String columnName = entry.getKey();
            String foreignKeyTable = entry.getValue();

            if (columns.containsKey(columnName)) {
                combinedMap.put(foreignKeyTable, columnName);
            }
        }

        for (Map.Entry<String, String> entry : combinedMap.entrySet()) {
            System.out.println("Key: " + entry.getKey());
        }

        res = template.replace("#table#", table)
                .replace("#API#", API)
                .replace("#path#", path)
                .replace("#column-title#", generateColumnTitle(combinedMap))
                .replace("#column-row#", generateColumnRows(combinedMap))
                .replace("#column-add#", generateColumnAdd(columns, foreignkeys, dbConnection))
                .replace("#column-edit#", generateColumnEdit(columns, foreignkeys, dbConnection))
                .replace("#ListeFK#", generateListeFK(foreignkeys))
                .replace("#column#", generateColumn(combinedMap, foreignkeys))
                .replace("#api-add-column#", generateApiAddColumn(combinedMap))
                .replace("#api-update-column#", generateApiUpdateColumn(combinedMap))
                .replace("#mounted-api-FK#", generateMountedApiFK(foreignkeys))
                .replace("#api-FK#", generateApiFK(foreignkeys, API))
                .replace("#selectedFKFunction#", generateSelectedFKFunction(foreignkeys))
                .replace("#selectedFK#", generateSelectedFK(foreignkeys))
                .replace("#csv-column", generateCsvColumn(combinedMap));
        return res;
    }

    private static String generateColumnTitle(HashMap<String, String> columns) {
        StringBuilder titleBuilder = new StringBuilder();
        for (String columnName : columns.keySet()) {
            titleBuilder.append("<th scope=\"col\">")
                    .append(ObjectUtility.capitalize(ObjectUtility.formatToCamelCase(columnName.replace("_", " "))))
                    .append("</th>\n\t\t\t\t\t\t\t");
        }
        return titleBuilder.toString();
    }

    private static String generateListeFK(HashMap<String, String> foreignKeys) {
        StringBuilder fkBuilder = new StringBuilder();
        for (String fk : foreignKeys.keySet()) {
            String fkWithoutId = foreignKeys.get(fk); // Supprimer les deux premiers caractères "id"
            String fkCamelCase = ObjectUtility.capitalize(ObjectUtility.formatToCamelCase(fkWithoutId)); // Convertir en
                                                                                                         // camelCase si
                                                                                                         // nécessaire
            String relatedTable = foreignKeys.get(fk);
            fkBuilder.append("liste").append(fkCamelCase).append(": [],").append("\n\t\t\t");
        }
        return fkBuilder.toString();
    }

    private static String generateColumnRows(HashMap<String, String> columns) {
        StringBuilder rowBuilder = new StringBuilder();
        for (String columnName : columns.keySet()) {
            rowBuilder.append("<td>{{ item.").append(ObjectUtility.formatToCamelCase(columnName))
                    .append(" }}</td>\n\t\t\t\t\t");
        }
        return rowBuilder.toString();
    }

    private static String generateColumn(HashMap<String, String> columns, HashMap<String, String> foreignKeys) {
        StringBuilder columnBuilder = new StringBuilder();
        for (String columnName : columns.keySet()) {
            if (foreignKeys.containsKey(columnName)) {
                columnBuilder.append(ObjectUtility.formatToCamelCase(columnName)).append(":{},\n\t\t\t");
            } else {
                columnBuilder.append(ObjectUtility.formatToCamelCase(columnName)).append(":'',\n\t\t\t");
            }
        }
        return columnBuilder.toString();
    }

    private static String generateColumnAdd(HashMap<String, String> columns, HashMap<String, String> foreignKeys,
            DbConnection dbConnection) {
        StringBuilder addBuilder = new StringBuilder();
        try {
            for (String columnName : columns.keySet()) {
                if (foreignKeys.containsKey(columnName)) {
                    List<String> PK = DbService.getPrimaryKey(dbConnection, foreignKeys.get(columnName));
                    addBuilder.append("\n\t\t\t\t\t\t<div class=\"mb-3\">")
                            .append("\n\t\t\t\t\t\t\t<label for=\"").append(foreignKeys.get(columnName))
                            .append("\" class=\"form-label\">")
                            .append(foreignKeys.get(columnName)).append("</label>")

                            .append("\n\t\t\t\t\t\t\t<select class=\"form-control\" id=\"")
                            .append(ObjectUtility.formatToCamelCase(foreignKeys.get(columnName)))
                            .append("\" v-model=\"")
                            .append(ObjectUtility.formatToCamelCase(foreignKeys.get(columnName))).append("\">")
                            .append("\n\t\t\t\t\t\t\t\t<option v-for=\"(option, index) in liste")
                            .append(ObjectUtility.capitalize(foreignKeys.get(columnName)))
                            .append("\" @click=\"select#FK#(option) ".replace("#FK#",
                                    ObjectUtility.capitalize(foreignKeys.get(columnName))))
                            .append("\" :key=\"index\" :value=\"option.#id#\">{{ option.#id# }}</option>".replace("#id#", PK.get(0)))
                            .append("\n\t\t\t\t\t\t\t</select>")
                            .append("\n\t\t\t\t\t\t</div>");
                } else {
                    // Si ce n'est pas une clé étrangère, générer un champ de texte
                    addBuilder.append("\n\t\t\t\t\t\t<div class=\"mb-3\">")
                            .append("\n\t\t\t\t\t\t\t<label for=\"").append(ObjectUtility.formatToCamelCase(columnName))
                            .append("\" class=\"form-label\">")
                            .append(ObjectUtility
                                    .capitalize(ObjectUtility.formatToCamelCase(columnName.replace("_", " "))))
                            .append("</label>")
                            .append("\n\t\t\t\t\t\t\t<input type=\"text\" class=\"form-control\" id=\"")
                            .append(ObjectUtility.formatToCamelCase(columnName)).append("\" v-model=\"")
                            .append(ObjectUtility.formatToCamelCase(columnName)).append("\">")
                            .append("\n\t\t\t\t\t\t</div>");
                    System.out.println(columnName);
                }
            }
        } catch (Exception e) {
        }
        return addBuilder.toString();
    }

    private static String generateColumnEdit(HashMap<String, String> columns, HashMap<String, String> foreignKeys,
            DbConnection dbConnection) {
        StringBuilder editBuilder = new StringBuilder();
        try {
        for (String columnName : columns.keySet()) {
            if (foreignKeys.containsKey(columnName)) {
                List<String> PK = DbService.getPrimaryKey(dbConnection, foreignKeys.get(columnName));
                editBuilder.append("\n\t\t\t\t\t\t<div class=\"mb-3\">")
                        .append("\n\t\t\t\t\t\t\t<label for=\"").append(foreignKeys.get(columnName))
                        .append("\" class=\"form-label\">")
                        .append(columnName).append("</label>")

                        .append("\n\t\t\t\t\t\t\t<select class=\"form-control\" id=\"")
                        .append(ObjectUtility.formatToCamelCase(foreignKeys.get(columnName)))
                        .append("\" v-model=\"selectedItem.")
                        .append(ObjectUtility.formatToCamelCase(foreignKeys.get(columnName))).append("\">")
                        .append("\n\t\t\t\t\t\t\t\t<option v-for=\"(option, index) in liste")
                        .append(ObjectUtility.capitalize(foreignKeys.get(columnName)))
                        .append("\" @click=\"select#FK#(option)".replace("#FK#",
                                ObjectUtility.capitalize(foreignKeys.get(columnName))))
                        .append("\" :key=\"index\" :value=\"option.#id#\">{{ option.#id# }}</option>".replace("#id#",PK.get(0)))
                        .append("\n\t\t\t\t\t\t\t</select>")
                        .append("\n\t\t\t\t\t\t</div>");
            } else {
                // Si ce n'est pas une clé étrangère, générer un champ de texte
                editBuilder.append("\n\t\t\t\t\t\t<div class=\"mb-3\">")
                        .append("\n\t\t\t\t\t\t\t<label for=\"").append(ObjectUtility.formatToCamelCase(columnName))
                        .append("\" class=\"form-label\">")
                        .append(columnName).append("</label>")
                        .append("\n\t\t\t\t\t\t\t<input type=\"text\" class=\"form-control\" id=\"")
                        .append(ObjectUtility.formatToCamelCase(columnName)).append("\" v-model=\"selectedItem.")
                        .append(ObjectUtility.formatToCamelCase(columnName)).append("\">")
                        .append("\n\t\t\t\t\t\t</div>");
            }
        }
        } catch (Exception e) {
    }
        return editBuilder.toString();
    }

    private static String generateApiUpdateColumn(HashMap<String, String> columns) {
        StringBuilder updateColumnBuilder = new StringBuilder();
        for (String columnName : columns.keySet()) {
            updateColumnBuilder.append(ObjectUtility.formatToCamelCase(columnName)).append(": this.selectedItem.")
                    .append(ObjectUtility.formatToCamelCase(columnName)).append(",\n\t\t\t\t");
        }

        return updateColumnBuilder.toString();
    }

    private static String generateCsvColumn(HashMap<String, String> columns) {
        StringBuilder addColumnBuilder = new StringBuilder();
        for (String columnName : columns.keySet()) {
            addColumnBuilder.append("this.").append(ObjectUtility.formatToCamelCase(columnName)).append(" = data.")
                    .append(ObjectUtility.formatToCamelCase(columnName)).append(";\n\t\t\t\t\t\t\t\t");
        }
        return addColumnBuilder.toString();
    }

    private static String generateApiAddColumn(HashMap<String, String> columns) {
        StringBuilder addColumnBuilder = new StringBuilder();
        for (String columnName : columns.keySet()) {
            addColumnBuilder.append(ObjectUtility.formatToCamelCase(columnName)).append(": this.")
                    .append(ObjectUtility.formatToCamelCase(columnName)).append(",\n\t\t\t\t\t\t\t\t");
        }
        return addColumnBuilder.toString();
    }

    private static String generateMountedApiFK(HashMap<String, String> foreignKeys) {
        StringBuilder mountedBuilder = new StringBuilder();
        for (String fk : foreignKeys.keySet()) {
            String fkCamelCase = ObjectUtility.formatToCamelCase(fk).substring(2);
            mountedBuilder.append("this.findAll").append(fkCamelCase).append("();\n\t\t");
        }
        return mountedBuilder.toString();
    }

    private static String generateSelectedFK(HashMap<String, String> foreignKeys) {
        StringBuilder selectedFKBuilder = new StringBuilder();
        for (String fk : foreignKeys.keySet()) {
            String FK = ObjectUtility.capitalize(ObjectUtility.formatToCamelCase(foreignKeys.get(fk)));
            selectedFKBuilder.append("selected").append(FK).append(": {},\n\t\t\t");
        }
        return selectedFKBuilder.toString();
    }

    private static String generateSelectedFKFunction(HashMap<String, String> foreignKeys) {
        StringBuilder selectedFKBuilder = new StringBuilder();
        for (String fk : foreignKeys.keySet()) {
            String FK = ObjectUtility.capitalize(ObjectUtility.formatToCamelCase(foreignKeys.get(fk)));
            selectedFKBuilder.append("select").append(FK).append("(item) {\n\t\t\t")
                    .append("this.selected").append(FK).append("= item;\n\t\t")
                    .append("},\n\t\t");
        }
        return selectedFKBuilder.toString();
    }

    private static String generateApiFK(HashMap<String, String> foreignKeys, String api) {
        StringBuilder fkMethodBuilder = new StringBuilder();
        for (String fk : foreignKeys.keySet()) {
            String fkCamelCase = ObjectUtility.formatToCamelCase(fk).substring(2);
            String relatedTable = foreignKeys.get(fk);
            fkMethodBuilder.append("async findAll").append(fkCamelCase).append("() {\n");
            fkMethodBuilder.append("\t\t\ttry {\n");
            fkMethodBuilder.append("\t\t\t\tconst response = await fetch('").append(api + "/").append(relatedTable)
                    .append("s").append("');\n");
            fkMethodBuilder.append("\t\t\t\tif (!response.ok) {\n");
            fkMethodBuilder.append("\t\t\t\t\tthrow new Error('Erreur de réseau');\n");
            fkMethodBuilder.append("\t\t\t\t}\n");
            fkMethodBuilder.append("\t\t\t\tthis.liste").append(ObjectUtility.capitalize(fkCamelCase))
                    .append(" = await response.json();\n");
            fkMethodBuilder.append("\t\t\t\tthis.liste#classFk# = this.liste#classFk#.data.body;".replace("#classFk#",
                    ObjectUtility.capitalize(fkCamelCase)));
            fkMethodBuilder.append("\t\t\t\t} catch (error) {\n");
            fkMethodBuilder.append("\t\t\t\t\tconsole.error('Erreur lors de la récupération :', error);\n");
            fkMethodBuilder.append("\t\t\t}\n");
            fkMethodBuilder.append("\t\t},\n\n\t\t");
        }
        return fkMethodBuilder.toString();
    }

    public static void generateAllViews(String[] tables, String API, DbConnection dbConnection)
            throws Exception {
        for (String table : tables) {
            String viewContent = generateView(table, API,
                    Misc.getViewJsTemplateLocation().concat(File.separator).concat("Page.template"),
                    dbConnection);
            String fileName = ObjectUtility.capitalize(ObjectUtility.formatToCamelCase(table)) + ".vue";
            FileUtility.createDirectory("Vue", "./");
            String path = "./Vue";
            FileUtility.generateFile(path, fileName, viewContent);
        }
    }
}
