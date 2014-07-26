/**
 * TESTING SERVICES
 */
 
namespace java org.sergilos.servicemanager.remote.test

service MathTestServiceAddition {
	i32 testingSum( 1:i32 val1, 2:i32 val2 ),
}

service MathTestServiceSubtraction{
	i32 testingSubtract( 1:i32 val1, 2:i32 val2 ),
}

service ThreadTestService {
	void testingWait( 1: i32 time ),
}
