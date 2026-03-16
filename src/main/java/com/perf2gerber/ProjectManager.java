package com.perf2gerber;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.perf2gerber.model.Board;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles saving and loading the Board model to/from JSON files.
 */
public class ProjectManager {

    // On configure Gson pour qu'il formate joliment le texte (PrettyPrinting)
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void saveBoard(Board board, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(board, writer);
        }
    }

    public static Board loadBoard(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, Board.class);
        }
    }
}