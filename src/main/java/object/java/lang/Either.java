package object.java.lang;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public interface Either <A, E extends Exception> {

	Either <A, E> ifSuccess (final Consumer<A> action);
	
	Either <A, E> ifFailure (final Consumer<E> action);
	
	@SuppressWarnings("unchecked")
	default <R, Y extends Exception> Either<R, Y> andThen (final Function<R, A, Y> fn, final java.util.function.Function<E, Y> eFn) {
		final AtomicReference<Either<R, Y>> result = new AtomicReference<>();
		ifSuccess(t -> {
			try {
				final R value = fn.code(t);
				result.set(new Success<>(value));
			} catch (final Exception e) {
				result.set(new Failure<>((Y) e));
			}
		}).ifFailure(oe -> result.set(new Failure<>(eFn.apply(oe))));
		return result.get();
	}
	
	interface Supplier<A, E extends Exception> {
		A code () throws E;
	}
	
	interface Function<A, R, E extends Exception> {
		A code(final R value) throws E;
	}
	
	@SuppressWarnings("unchecked")
	public static <T, X extends Exception> Either <T, X> wrap (final Supplier <T, X> th) {
		try {
			final T value = th.code();
			return new Success<>(value);
		} catch (final Exception excep) {
			return new Failure<>((X) excep);
		}
	}
	
	public static <T, X extends Exception> Either <T, X> fail (final X exception) {
		return new Failure<>(exception);
	}
	
	public static <T, X extends Exception> Either<T, X> succ (final T value) {
		return new Success<>(value);
	}
	
	final static class Failure<A, E extends Exception> implements Either<A, E> {

		private final E excep;

		Failure (final E excep) {
			this.excep = excep;
		}
		
		@Override
		public Either<A, E> ifSuccess(final Consumer<A> action) {
			return this;
		}

		@Override
		public Either<A, E> ifFailure(final Consumer<E> action) {
			action.accept(excep);
			return this;
		}		
	}
	
	final static class Success<A, E extends Exception> implements Either <A, E> {

		private final A value;

		Success(final A value) {
			this.value = value;
		}
		
		@Override
		public Either<A, E> ifSuccess(final Consumer<A> action) {
			action.accept(value);
			return this;
		}

		@Override
		public Either<A, E> ifFailure(final Consumer<E> action) {
			return this;
		}		
	}
}
