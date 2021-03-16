package com.stirante.json.utils;

import com.stirante.justpipe.Pipe;
import com.stirante.justpipe.function.IOFunction;
import org.json.JSONObject;

public class PipeExtensions {

    public static IOFunction<Pipe, JSONObject> JSON_OBJECT = pipe -> new JSONObject(pipe.toString());

}
