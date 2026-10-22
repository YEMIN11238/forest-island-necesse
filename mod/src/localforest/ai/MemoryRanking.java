package localforest.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Java port of AI Town rankAndTouchMemories' relevance + importance + recency ranking.
 * Source: a16z-infra/ai-town, 8e05997f2409275669c8344b84a51692e83f3f33 (MIT).
 * Copyright (c) 2023 a16z-infra. See resources/third-party/ai-town/LICENSE.
 * The game supplies observed facts and its own clock; no model, embedding service or DB is required.
 * Adaptations: stable ties, finite-input validation, empty/uniform ranges, no implicit persistence.
 */
public final class MemoryRanking {
    public record Candidate<T>(String id, T value, double relevance, double importance, long lastAccessMillis) {
        public Candidate {
            Objects.requireNonNull(id); Objects.requireNonNull(value);
            if (!Double.isFinite(relevance) || !Double.isFinite(importance)) throw new IllegalArgumentException("Non-finite memory score");
        }
    }
    private record Scored<T>(Candidate<T> candidate, double score) {}
    private static double normalize(double value, double min, double max) { return max == min ? 0 : (value - min) / (max - min); }

    /** Caller must filter by world, companion and observed knowledge before ranking. */
    public static <T> List<T> rank(List<Candidate<T>> candidates, long nowMillis, int limit) {
        if (limit <= 0 || candidates.isEmpty()) return List.of();
        double minR=Double.POSITIVE_INFINITY,maxR=Double.NEGATIVE_INFINITY;
        double minI=minR,maxI=maxR,minT=minR,maxT=maxR;
        double[] recency = new double[candidates.size()];
        for(int i=0;i<candidates.size();i++) {
            Candidate<T> c=candidates.get(i);
            minR=Math.min(minR,c.relevance()); maxR=Math.max(maxR,c.relevance());
            minI=Math.min(minI,c.importance()); maxI=Math.max(maxI,c.importance());
            recency[i]=Math.pow(.99,Math.floor(Math.max(0.0,(double)nowMillis-c.lastAccessMillis())/3_600_000));
            minT=Math.min(minT,recency[i]); maxT=Math.max(maxT,recency[i]);
        }
        List<Scored<T>> scored=new ArrayList<>();
        for(int i=0;i<candidates.size();i++) {
            Candidate<T> c=candidates.get(i);
            scored.add(new Scored<>(c,normalize(c.relevance(),minR,maxR)+normalize(c.importance(),minI,maxI)+normalize(recency[i],minT,maxT)));
        }
        scored.sort(Comparator.<Scored<T>>comparingDouble(Scored::score).reversed().thenComparing(s->s.candidate().id()));
        return scored.stream().limit(limit).map(s->s.candidate().value()).toList();
    }
    private MemoryRanking() {}
}
