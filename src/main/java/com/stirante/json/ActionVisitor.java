package com.stirante.json;

import com.stirante.json.utils.JsonUtils;
import org.json.JSONObject;

public class ActionVisitor extends SimpleReferenceBaseVisitor<ReferenceResult> {
    private final JSONObject extraScope;
    private final JSONObject fullScope;
    private final Object currentScope;
    private final String path;

    public ActionVisitor(JSONObject extraScope, JSONObject fullScope, Object currentScope, String path) {
        this.extraScope = extraScope;
        this.fullScope = fullScope;
        this.currentScope = currentScope;
        this.path = path;
    }

    @Override
    public ReferenceResult visitAction(SimpleReferenceParser.ActionContext ctx) {
        JsonProcessor.Action a = JsonProcessor.Action.VALUE;
        if (ctx.Iteration() != null) {
            a = JsonProcessor.Action.ITERATION;
        }
        else if (ctx.AsInt() != null) {
            a = JsonProcessor.Action.AS_INT;
        }
        else if (ctx.Predicate() != null) {
            a = JsonProcessor.Action.PREDICATE;
            if (ctx.reference().size() > 1) {
                Object predicateResult =
                        new ReferenceVisitor(extraScope, fullScope, currentScope, path, a).visit(ctx.reference(0));
                if (JsonUtils.toBoolean(predicateResult)) {
                    return new ReferenceResult(new ReferenceVisitor(extraScope, fullScope, currentScope, path, a)
                            .visit(ctx.reference(1)), a,
                            ctx.As() != null ? ctx.name().getText() : "value");
                }
                else if (ctx.reference().size() == 3) {
                    return new ReferenceResult(new ReferenceVisitor(extraScope, fullScope, currentScope, path, a)
                            .visit(ctx.reference(2)), a,
                            ctx.As() != null ? ctx.name().getText() : "value");
                }
            }
        }
        return new ReferenceResult(new ReferenceVisitor(extraScope, fullScope, currentScope, path, a)
                .visit(ctx.reference(0)), a,
                ctx.As() != null ? ctx.name().getText() : "value");
    }
}
