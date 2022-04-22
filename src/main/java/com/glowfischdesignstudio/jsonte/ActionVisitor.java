package com.glowfischdesignstudio.jsonte;

import org.json.JSONObject;

import java.util.Deque;

public class ActionVisitor extends JsonTemplateBaseVisitor<ReferenceResult> {
    private final JSONObject extraScope;
    private final JSONObject fullScope;
    private final Deque<Object> currentScope;
    private final String path;

    public ActionVisitor(JSONObject extraScope, JSONObject fullScope, Deque<Object> currentScope, String path) {
        this.extraScope = extraScope;
        this.fullScope = fullScope;
        this.currentScope = currentScope;
        this.path = path;
    }

    @Override
    public ReferenceResult visitAction(JsonTemplateParser.ActionContext ctx) {
        JsonAction a = JsonAction.VALUE;
        if (ctx.Iteration() != null) {
            a = JsonAction.ITERATION;
        }
        else if (ctx.Literal() != null) {
            a = JsonAction.LITERAL;
        }
        else if (ctx.Question() != null) {
            a = JsonAction.PREDICATE;
        }
        return new ReferenceResult(new FieldVisitor(extraScope, fullScope, currentScope, path, a)
                .visit(ctx.field()), a,
                ctx.As() != null ? ctx.name().getText() : "value");
    }
}
